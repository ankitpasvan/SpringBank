package com.example.banking.exception;

/**
 * Thrown for any JWT that cannot be trusted: expired, tampered with (bad signature),
 * or not a well-formed JWT at all. Deliberately one exception type for all three cases:
 * the caller (the security filter added in Step 4) only needs to know "reject this request",
 * not which specific way the token was invalid.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
