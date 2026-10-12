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

        IdempotencyRecord claim = idempotencyService.claim("user-1", "key-1", "fingerprint-1");
        String recordId = claim.getId();
        assertTrue(idempotencyRecordRepository.findById(recordId).isPresent());


        TransactionTemplate businessTx = new TransactionTemplate(transactionManager);
        assertThrows(IllegalStateException.class, () -> businessTx.execute(status -> {
            idempotencyService.release(recordId);
            status.setRollbackOnly();
            throw new IllegalStateException("simulated business failure");
        }));

        entityManager.clear();

        assertTrue(idempotencyRecordRepository.findById(recordId).isEmpty());
    }

    @Test
    void release_unknownId_doesNotThrow() {

        idempotencyService.release("no-such-record");
        assertEquals(0, idempotencyRecordRepository.count());
    }
}
