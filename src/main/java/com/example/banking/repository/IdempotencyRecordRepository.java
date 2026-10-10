package com.example.banking.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.example.banking.model.IdempotencyRecord;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {

    Optional<IdempotencyRecord> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);

    // Removes records whose 24-hour retention has expired. Replaces the MongoDB TTL index,
    // which has no relational equivalent - see IdempotencyService.evictExpiredRecords.
    @Transactional
    void deleteByExpiresAtBefore(Instant cutoff);
}
