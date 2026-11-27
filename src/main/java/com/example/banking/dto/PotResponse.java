package com.example.banking.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.banking.model.SavingsPot;

public record PotResponse(
        String id,
        String name,
        String currency,
        BigDecimal targetAmount,
        BigDecimal balance,
        String icon,
        Instant createdAt,
        Instant updatedAt
) {

    public static PotResponse from(SavingsPot pot) {
        return new PotResponse(
                pot.getId(), pot.getName(), pot.getCurrency(), pot.getTargetAmount(),
                pot.getBalance(), pot.getIcon(), pot.getCreatedAt(), pot.getUpdatedAt());
    }
}