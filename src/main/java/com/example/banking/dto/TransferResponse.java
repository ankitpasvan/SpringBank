package com.example.banking.dto;

import java.math.BigDecimal;
import java.time.Instant;


public record TransferResponse(
        String transactionId,
        String fromAccountId,
        String toAccountId,
        BigDecimal amount,
        BigDecimal senderBalanceAfter,
        String description,
        Instant createdAt) {
}