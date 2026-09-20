package com.example.banking.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.banking.model.Account;
import com.example.banking.model.AccountType;

/**
 * JSON returned to the client for an account.
 */
public record AccountResponse(
        String id,
        String accountNumber,
        String userId,
        AccountType accountType,
        BigDecimal balance,
        Instant createdAt,
        Instant updatedAt) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getUserId(),
                account.getAccountType(),
                account.getBalance(),
                account.getCreatedAt(),
                account.getUpdatedAt());
    }
}
