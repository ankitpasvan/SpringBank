package com.example.banking.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "idempotency_records",
        uniqueConstraints = @UniqueConstraint(name = "user_key_idx",
                columnNames = { "userId", "idempotencyKey" }),
        indexes = @Index(name = "idempotency_expires_idx", columnList = "expiresAt"))
public class IdempotencyRecord {

    private static final long RETENTION_SECONDS = 86_400; // 24 hours

    @Id
    @UuidGenerator
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IdempotencyStatus status;

    // SHA-256 over the normalized request (account, operation, amount, description).
    // Null only for records written before fingerprinting existed.
    private String requestFingerprint;

    // Set only once the underlying operation has actually completed.
    private String resultTransactionId;

    // Only meaningful for a transfer (the Transaction row itself has no "toAccountId" field);
    // stays null for deposit/withdraw.
    private String toAccountId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Rows with expiresAt in the past are deleted by the scheduled cleanup task.
    @Column(nullable = false)
    private Instant expiresAt;

    /** Required by JPA. */
    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(String userId, String idempotencyKey, String requestFingerprint) {
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.status = IdempotencyStatus.IN_PROGRESS;
        Instant now = Instant.now();
        this.createdAt = now;
        this.expiresAt = now.plusSeconds(RETENTION_SECONDS);
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

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    /**
     * Sets the fingerprint exactly once, for legacy records that were written before
     * fingerprinting existed. Never overwrites an already stored fingerprint - callers must
     * check {@link #getRequestFingerprint()} first (see IdempotencyService.ensureFingerprint).
     */
    public void setRequestFingerprint(String requestFingerprint) {
        this.requestFingerprint = requestFingerprint;
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

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
