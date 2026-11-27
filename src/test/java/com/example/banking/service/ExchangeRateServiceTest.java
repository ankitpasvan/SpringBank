package com.example.banking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ExchangeRateServiceTest {

    private final ExchangeRateService service = new ExchangeRateService();

    @Test
    void sameCurrencyRateIsOne() {
        assertEquals(0, service.getRate("INR", "INR").compareTo(BigDecimal.ONE));
        assertEquals(0, service.getRate("USD", "USD").compareTo(BigDecimal.ONE));
    }

    @Test
    void feeIsZeroPointFourNinePercent() {
        assertEquals(0, service.feeFor(new BigDecimal("10000")).compareTo(new BigDecimal("49.00")));
        assertEquals(0, service.feeFor(BigDecimal.ZERO).compareTo(BigDecimal.ZERO));
    }

    @Test
    void supportedCurrenciesContainInrAndUsd() {
        assertTrue(service.supportedCurrencies().contains("INR"));
        assertTrue(service.supportedCurrencies().contains("USD"));
        assertTrue(service.supportedCurrencies().contains("EUR"));
    }

    @Test
    void inrToUsdRateIsPositive() {
        BigDecimal rate = service.getRate("INR", "USD");
        assertTrue(rate.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(rate.compareTo(new BigDecimal("0.02")) < 0);
    }
}