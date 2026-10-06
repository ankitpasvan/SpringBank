package com.example.banking.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Tracks one client-supplied "Idempotency-Key" for one user, so a retried request can be
 * detected and replayed instead of re-executed. Stored in the "idempotency_records" collection.
 *
 * WHY scoped to (userId, idempotencyKey): a key is only unique PER USER, never globally - two
 * different users can safely reuse the exact same key string without ever colliding, and one
 * user can never see or replay another user's result through this mechanism.
 *
 * WHY a 24-hour TTL: an abandoned IN_PROGRESS record (from a genuine process crash between the
 * claim and the business operation completing) would otherwise block that key forever. This is
 * a deliberate simplification: it bounds crash recovery to at most 24 hours rather than
 * guaranteeing instant recovery - an acceptable, explainable trade-off for this project, and
 * the same retention window several real payment APIs use for idempotency keys.
 */
@Document(collection = "idempotency_records")
@CompoundIndex(name = "user_key_idx", def = "{'userId': 1, 'idempotencyKey': 1}", unique = true)
public class IdempotencyRecord {

    @Id
    private String id;

    private String userId;

    private String idempotencyKey;

    private IdempotencyStatus status;

    // Set only once the underlying operation has actually completed.
    private String resultTransactionId;

    // Only meaningful for a transfer (the Transaction row itself has no "toAccountId" field);
    // stays null for deposit/withdraw.
    private String toAccountId;

    @Indexed(expireAfterSeconds = 86_400)
    private Instant createdAt;

    public IdempotencyRecord(String userId, String idempotencyKey) {
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.status = IdempotencyStatus.IN_PROGRESS;
        this.createdAt = Instant.now();
    }

    public void markCompleted(String resultTransactionId, String toAccountId) {
        this.status = IdempotencyStatus.COMPLETED;
        this.resultTransactionId = resultTransactionId;
        this.toAccountId = toAccountId;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public String getResultTransactionId() {
        return resultTransactionId;
    }

    public String getToAccountId() {
        return toAccountId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}