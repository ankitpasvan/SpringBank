package com.example.banking.exception;

/**
 * Thrown when login fails, for EITHER an unknown email OR a wrong password.
 * Both cases use the same message on purpose: if "email not found" and "wrong password"
 * returned different messages, an attacker could use the API to find out which emails
 * are registered (this is called "user enumeration").
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
