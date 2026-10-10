package com.example.banking.exception;

/**
 * Thrown when a request reuses an idempotency key whose first use had a DIFFERENT normalized
 * payload (different amount or description). Mapped to 409 Conflict: replaying the old
 * result would lie to the caller, and re-executing would move money twice, so the client
 * must retry the genuinely different request with a fresh key.
 */
public class IdempotencyKeyConflictException extends RuntimeException {

    public IdempotencyKeyConflictException() {
        super("This idempotency key was already used with a different request");
    }

    public IdempotencyKeyConflictException(String message) {
        super(message);
    }
}
