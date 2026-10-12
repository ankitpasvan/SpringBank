package com.example.banking.exception;


public class IdempotencyKeyConflictException extends RuntimeException {

    public IdempotencyKeyConflictException() {
        super("This idempotency key was already used with a different request");
    }

    public IdempotencyKeyConflictException(String message) {
        super(message);
    }
}
