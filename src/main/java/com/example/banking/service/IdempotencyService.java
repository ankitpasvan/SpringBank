package com.example.banking.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.example.banking.exception.IdempotencyInProgressException;
import com.example.banking.model.IdempotencyRecord;
import com.example.banking.model.IdempotencyStatus;
import com.example.banking.repository.IdempotencyRecordRepository;

/**
 * Foundation for turning a client-supplied "Idempotency-Key" header into a safe retry
 * mechanism. This class knows nothing about deposits/withdrawals/transfers - it only answers
 * "has this (userId, idempotencyKey) been claimed before, and if so what happened?" The
 * money-moving services (wired up in a later step) decide what to DO with that answer.
 *
 * WHY claim() can be called concurrently without a race: uniqueness is enforced by a MongoDB
 * unique index on (userId, idempotencyKey), not by application-level locking. Two simultaneous
 * callers both attempt an insert; the database allows only one to succeed, and the loser's
 * DuplicateKeyException is exactly the information needed to react correctly - the same
 * pattern already used for duplicate emails (UserService) and account-number collisions
 * (AccountService) elsewhere in this project.
 */
@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public IdempotencyService(IdempotencyRecordRepository idempotencyRecordRepository) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
    }

    /**
     * Attempts to claim this (userId, idempotencyKey) pair for a new operation.
     *
     * @return a freshly created IN_PROGRESS record if the claim succeeded (the caller should
     *         now execute the real operation, then call markCompleted with this record's id),
     *         or a previously COMPLETED record if this exact key already finished before (the
     *         caller should replay that result instead of executing anything).
     * @throws IdempotencyInProgressException if another, not-yet-finished request already
     *         owns this exact key right now.
     */
    public IdempotencyRecord claim(String userId, String idempotencyKey) {
        try {
            return idempotencyRecordRepository.save(new IdempotencyRecord(userId, idempotencyKey));
        } catch (DuplicateKeyException ex) {
            IdempotencyRecord existing = idempotencyRecordRepository
                    .findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                    .orElseThrow(() -> ex);

            if (existing.getStatus() == IdempotencyStatus.IN_PROGRESS) {
                throw new IdempotencyInProgressException();
            }

            return existing;
        }
    }

    /** Marks a claimed record as completed, storing what later replays should return. */
    public void markCompleted(String recordId, String resultTransactionId, String toAccountId) {
        IdempotencyRecord record = idempotencyRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalStateException("Idempotency record not found: " + recordId));
        record.markCompleted(resultTransactionId, toAccountId);
        idempotencyRecordRepository.save(record);
    }

    /**
     * Deletes an IN_PROGRESS claim - e.g. because the operation it was guarding failed with a
     * normal business error (insufficient funds, account not found, ...). This lets a later
     * retry with the same key start cleanly instead of being permanently stuck.
     */
    public void release(String recordId) {
        idempotencyRecordRepository.deleteById(recordId);
    }
}