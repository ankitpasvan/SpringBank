package com.example.banking.service;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.banking.exception.IdempotencyInProgressException;
import com.example.banking.model.IdempotencyRecord;
import com.example.banking.model.IdempotencyStatus;
import com.example.banking.repository.IdempotencyRecordRepository;


@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    // Self-injection (via the Spring proxy) so findExistingClaim() below really runs in its
    // own fresh transaction - a direct this.findExistingClaim() call would bypass the proxy
    // and silently inherit the poisoned transaction of the catch block.
    private final IdempotencyService self;

    public IdempotencyService(IdempotencyRecordRepository idempotencyRecordRepository) {
        this(idempotencyRecordRepository, null);
    }

    @Autowired
    public IdempotencyService(IdempotencyRecordRepository idempotencyRecordRepository,
                              @Lazy IdempotencyService self) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.self = self;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyRecord claim(String userId, String idempotencyKey, String requestFingerprint) {
        try {
            IdempotencyRecord record =
                    idempotencyRecordRepository.save(new IdempotencyRecord(userId, idempotencyKey,
                            requestFingerprint));
            idempotencyRecordRepository.flush();
            return record;
        } catch (DuplicateKeyException ex) {

            IdempotencyService proxy = (self != null) ? self : this;
            return proxy.findExistingClaim(userId, idempotencyKey);
        }
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyRecord findExistingClaim(String userId, String idempotencyKey) {
        IdempotencyRecord existing = idempotencyRecordRepository
                .findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .orElseThrow(() -> new IllegalStateException(
                        "Idempotency claim vanished for key: " + idempotencyKey));

        if (existing.getStatus() == IdempotencyStatus.IN_PROGRESS) {
            throw new IdempotencyInProgressException();
        }

        return existing;
    }


    public String ensureFingerprint(String recordId, String fingerprint) {
        IdempotencyRecord record = idempotencyRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalStateException("Idempotency record not found: " + recordId));
        if (record.getRequestFingerprint() != null) {
            return record.getRequestFingerprint();
        }
        record.setRequestFingerprint(fingerprint);
        idempotencyRecordRepository.save(record);
        return fingerprint;
    }

    /** Marks a claimed record as completed, storing what later replays should return. */
    public void markCompleted(String recordId, String resultTransactionId, String toAccountId) {
        IdempotencyRecord record = idempotencyRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalStateException("Idempotency record not found: " + recordId));
        record.markCompleted(resultTransactionId, toAccountId);
        idempotencyRecordRepository.save(record);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(String recordId) {
        idempotencyRecordRepository.deleteById(recordId);
    }


    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void evictExpiredRecords() {
        idempotencyRecordRepository.deleteByExpiresAtBefore(Instant.now());
    }
}
