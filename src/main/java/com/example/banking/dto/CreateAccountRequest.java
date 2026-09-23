package com.example.banking.dto;

import com.example.banking.model.AccountType;

import jakarta.validation.constraints.NotNull;

/**
 * Incoming JSON for POST /api/accounts.
 * The owner is no longer taken from this body - it comes from the authenticated
 * principal (the JWT's userId), so a caller can never create an account for someone else.
 */
public record CreateAccountRequest(

        @NotNull(message = "Account type is required")
        AccountType accountType) {
}