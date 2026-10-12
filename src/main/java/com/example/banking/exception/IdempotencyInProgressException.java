package com.example.banking.exception;


public class IdempotencyInProgressException extends RuntimeException {

    public IdempotencyInProgressException() {
        super("A request with this idempotency key is already being processed");
    }
}