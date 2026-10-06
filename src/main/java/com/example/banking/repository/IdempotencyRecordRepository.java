package com.example.banking.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.banking.model.IdempotencyRecord;

public interface IdempotencyRecordRepository extends MongoRepository<IdempotencyRecord, String> {

    Optional<IdempotencyRecord> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);
}