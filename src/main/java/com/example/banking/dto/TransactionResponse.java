package com.example.banking.dto;



import java.math.BigDecimal;
import java.time.Instant;

import com.example.banking.model.Transaction;
import com.example.banking.model.TransactionType;

public record TransactionResponse(
        String id,
        String accountId,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        Instant createdAt) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getDescription(),
                transaction.getCreatedAt());
    }
}
