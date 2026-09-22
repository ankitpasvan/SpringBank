package com.example.banking.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Incoming JSON for POST /api/auth/login (endpoint added in Step 3).
 * No @Email or @Size rules here on purpose: at login time we must not tell the client
 * anything about why a credential is wrong, so even an obviously malformed email
 * is simply checked against the database and rejected the same way as any other mismatch.
 */
public record LoginRequest(

        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Password is required")
        String password) {
}