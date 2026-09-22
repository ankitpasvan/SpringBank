package com.example.banking.exception;

/**
 * The request is syntactically fine, but a business/parameter rule is broken
 * (for example page size too large, or "from" date after "to" date). Mapped to HTTP 400.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
