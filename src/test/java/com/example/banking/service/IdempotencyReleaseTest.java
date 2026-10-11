package com.example.banking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.banking.model.IdempotencyRecord;
import com.example.banking.repository.IdempotencyRecordRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

// Real transaction-behavior test for the idempotency release path. H2 behaves like
// PostgreSQL for what matters here: a rolled-back transaction undoes everything it did.
//
// WHY this test exists: TransactionService and TransferService call release() from inside
// their catch block - i.e. inside a business transaction that is about to roll back
// (unknown account, insufficient funds, ...). If release() joined that transaction, the
// DELETE would be rolled back as well, while the IN_PROGRESS row (committed separately
// by claim()) would survive - leaving the key stuck at 409 "in progress" for up to 24
// hours and defeating the whole purpose of safe retries. release() must therefore delete
// in its own REQUIRES_NEW transaction. Mock-based unit tests cannot see this: a Mockito
// mock has no transaction to roll back. This test pins the real behavior - without the
// REQUIRES_NEW on release(), the row survives the rollback and the final assertion fails.
@DataJpaTest
@Import(IdempotencyService.class)
class IdempotencyReleaseTest {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void release_insideRollingBackCallerTransaction_stillDeletesTheClaim() {
        // claim() commits the IN_PROGRESS row in its own transaction, exactly like production.
        IdempotencyRecord claim = idempotencyService.claim("user-1", "key-1", "fingerprint-1");
        String recordId = claim.getId();
        assertTrue(idempotencyRecordRepository.findById(recordId).isPresent());

        // Simulate the service catch block: release() is invoked inside a business
        // transaction that then fails and rolls back.
        TransactionTemplate businessTx = new TransactionTemplate(transactionManager);
        assertThrows(IllegalStateException.class, () -> businessTx.execute(status -> {
            idempotencyService.release(recordId);
            status.setRollbackOnly();
            throw new IllegalStateException("simulated business failure");
        }));

        // The DELETE committed in its own transaction, so it survived the rollback.
        // Clear this test's persistence context BEFORE verifying: the findById above
        // cached the entity here, and JPA would otherwise serve that stale copy instead
        // of checking the database (the DELETE happened in a different transaction).
        // This keeps the assertion honest - it now verifies the real database state.
        entityManager.clear();
        // A retry with the same Idempotency-Key now starts clean instead of hitting 409.
        assertTrue(idempotencyRecordRepository.findById(recordId).isEmpty());
    }

    @Test
    void release_unknownId_doesNotThrow() {
        // deleteById on a missing row must not blow up the caller's catch block.
        idempotencyService.release("no-such-record");
        assertEquals(0, idempotencyRecordRepository.count());
    }
}
