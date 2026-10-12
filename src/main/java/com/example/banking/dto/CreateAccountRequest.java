package com.example.banking.dto;

import com.example.banking.model.AccountType;

import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(

        @NotNull(message = "Account type is required")
        AccountType accountType) {
}