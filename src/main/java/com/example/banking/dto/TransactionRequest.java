package com.example.banking.dto;


import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Incoming JSON for POST /api/accounts/{accountId}/deposit and /withdraw.
 */
public record TransactionRequest(

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        @DecimalMax(value = "1000000.00", message = "Amount must not exceed 1000000.00")
        @Digits(integer = 7, fraction = 2, message = "Amount can have at most 2 decimal places")
        BigDecimal amount,

        @Size(max = 100, message = "Description must be at most 100 characters")
        String description) {
}
