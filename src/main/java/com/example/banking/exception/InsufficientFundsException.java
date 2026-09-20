package com.example.banking.exception;


public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException() {
        super("Insufficient funds for this withdrawal");
    }
}
