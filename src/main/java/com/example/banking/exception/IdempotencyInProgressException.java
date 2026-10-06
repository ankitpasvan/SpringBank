package com.example.banking.exception;

/**
 * Thrown when a client retries a request with an Idempotency-Key that an earlier,
 * not-yet-completed request is still processing. Mapped to 409 Conflict once this is wired
 * into a controller (a later step) rather than silently executing a second time.
 */
public class IdempotencyInProgressException extends RuntimeException {

    public IdempotencyInProgressException() {
        super("A request with this idempotency key is already being processed");
    }
}