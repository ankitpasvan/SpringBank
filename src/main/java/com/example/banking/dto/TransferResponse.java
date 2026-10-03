package com.example.banking.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JSON returned by POST /api/accounts/{accountId}/transfer.
 * Only the SENDER's own balance is exposed - the receiver's balance is their own
 * private information, not something the sender is entitled to see.
 */
public record TransferResponse(
        String transactionId,
        String fromAccountId,
        String toAccountId,
        BigDecimal amount,
        BigDecimal senderBalanceAfter,
        String description,
        Instant createdAt) {
}