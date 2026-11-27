package com.example.banking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record ExchangeRateResponse(
        String base,
        Map<String, BigDecimal> rates,
        Instant fetchedAt,
        boolean fallback,
        String source
) {
}