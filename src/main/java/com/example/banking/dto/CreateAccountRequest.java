package com.example.banking.dto;

import com.example.banking.model.AccountType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(

        @NotBlank(message = "User id is required")
        String userId,

        @NotNull(message = "Account type is required")
        AccountType accountType
) {
}