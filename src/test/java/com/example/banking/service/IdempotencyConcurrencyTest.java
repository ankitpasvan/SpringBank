package com.example.banking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.banking.dto.TransactionResponse;
import com.example.banking.exception.IdempotencyInProgressException;
import com.example.banking.model.Account;
import com.example.banking.model.AccountType;
import com.example.banking.model.IdempotencyRecord;
import com.example.banking.model.Transaction;
import com.example.banking.repository.AccountBalanceRepository;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.IdempotencyRecordRepository;
import com.example.banking.repository.TransactionRepository;

/**
 * Mock-level concurrency coverage for the idempotency claim race. A REAL
 * {@link IdempotencyService} is wired to a fake repository whose save() enforces the unique
 * (userId, idempotencyKey) constraint under a lock, exactly like MongoDB's unique index
 * would - so simultaneous claims genuinely collide and only one caller wins.
 *
 * <p>This proves the APPLICATION-level race handling (duplicate-key -> single winner).
 * It does NOT prove the real MongoDB unique index behaves the same way; that needs an
 * integration test against a real database.
 */
@ExtendWith(MockitoExtension.class)
class IdempotencyConcurrencyTest {

    private static final String ACCOUNT_ID = "acc-1";
    private static final String USER_ID = "user-1";
    private static final String KEY = "key-concurrent";

    @Mock
    private AccountBalanceRepository accountBalanceRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Test
    void concurrentDeposits_sameKey_executeFinancialOperationExactlyOnce() throws Exception {
        // Fake idempotency collection: unique (userId, key) enforced under one lock.
        Map<String, IdempotencyRecord> byUserKey = new HashMap<>();
        Map<String, IdempotencyRecord> byId = new HashMap<>();
        Object lock = new Object();
        AtomicInteger idSeq = new AtomicInteger();

        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class))).thenAnswer(inv -> {
            IdempotencyRecord record = inv.getArgument(0);
            synchronized (lock) {
                if (record.getId() != null && byId.containsKey(record.getId())) {
                    // Update of an already-persisted document (e.g. markCompleted): real MongoDB
                    // replaces by _id here, so the unique (userId, key) index is never violated.
                    // Only genuine inserts go through the duplicate-key check below.
                    byUserKey.put(record.getUserId() + "|" + record.getIdempotencyKey(), record);
                    byId.put(record.getId(), record);
                    return record;
                }
                String key = record.getUserId() + "|" + record.getIdempotencyKey();
                if (byUserKey.containsKey(key)) {
                    throw new DuplicateKeyException("duplicate key");
                }
                ReflectionTestUtils.setField(record, "id", "rec-" + idSeq.incrementAndGet());
                byUserKey.put(key, record);
                byId.put(record.getId(), record);
                return record;
            }
        });
        // Lenient: whether losers hit these depends on thread timing.
        lenient().when(idempotencyRecordRepository.findByUserIdAndIdempotencyKey(any(), any()))
                .thenAnswer(inv -> {
                    synchronized (lock) {
                        return Optional.ofNullable(
                                byUserKey.get(inv.getArgument(0) + "|" + inv.getArgument(1)));
                    }
                });
        lenient().when(idempotencyRecordRepository.findById(any()))
                .thenAnswer(inv -> {
                    synchronized (lock) {
                        return Optional.ofNullable(byId.get(inv.getArgument(0)));
                    }
                });

        AtomicInteger executions = new AtomicInteger();
        when(accountBalanceRepository.deposit(any(), any(), any())).thenAnswer(inv -> {
            executions.incrementAndGet();
            // Widen the race window so threads genuinely overlap inside the claim.
            Thread.sleep(50);
            return Optional.of(new Account("123456789012", USER_ID, AccountType.SAVINGS,
                    new BigDecimal("1010.00")));
        });

        AtomicReference<Transaction> savedTx = new AtomicReference<>();
        AtomicInteger txSeq = new AtomicInteger();
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction tx = inv.getArgument(0);
            ReflectionTestUtils.setField(tx, "id", "tx-" + txSeq.incrementAndGet());
            savedTx.set(tx);
            return tx;
        });
        lenient().when(transactionRepository.findById(any())).thenAnswer(inv -> {
            Transaction tx = savedTx.get();
            if (tx == null) {
                // A loser can only reach the replay path after the winner completed, which is
                // strictly after the ledger save; the re-read covers visibility across threads.
                Thread.sleep(200);
                tx = savedTx.get();
            }
            return Optional.ofNullable(tx);
        });

        IdempotencyService idempotencyService = new IdempotencyService(idempotencyRecordRepository);
        TransactionService transactionService = new TransactionService(accountBalanceRepository,
                accountRepository, transactionRepository, idempotencyService);

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<TransactionResponse>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return transactionService.deposit(ACCOUNT_ID, USER_ID, new BigDecimal("10.00"), "note", KEY);
            }));
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "threads did not finish");

        int succeeded = 0;
        for (Future<TransactionResponse> future : futures) {
            try {
                future.get();
                succeeded++;
            } catch (Exception ex) {
                // Losers either collide while the winner is IN_PROGRESS (409-style) or arrive
                // after completion and replay - both are correct, neither moves money again.
                assertTrue(ex.getCause() instanceof IdempotencyInProgressException,
                        "unexpected failure: " + ex.getCause());
            }
        }

        assertEquals(1, executions.get(), "financial operation must execute exactly once");
        assertTrue(succeeded >= 1, "at least the winning request must succeed");
    }
}
