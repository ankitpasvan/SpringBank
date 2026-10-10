package com.example.banking.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.Test;

import com.example.banking.model.TransactionType;

class IdempotencyFingerprintTest {

    private static BigDecimal money(String value) {
        // Same normalization the service applies before fingerprinting.
        return new BigDecimal(value).setScale(2, RoundingMode.UNNECESSARY);
    }

    @Test
    void sameInputs_produceSameFingerprint() {
        String first = IdempotencyFingerprint.of("acc-1", TransactionType.DEPOSIT, money("100.00"), "salary");
        String second = IdempotencyFingerprint.of("acc-1", TransactionType.DEPOSIT, money("100.00"), "salary");

        assertEquals(first, second);
    }

    @Test
    void equivalentAmountRepresentations_produceSameFingerprint() {
        String fromInt = IdempotencyFingerprint.of("acc-1", TransactionType.DEPOSIT, money("100"), "salary");
        String fromOneDecimal = IdempotencyFingerprint.of("acc-1", TransactionType.DEPOSIT, money("100.0"), "salary");
        String fromTwoDecimals = IdempotencyFingerprint.of("acc-1", TransactionType.DEPOSIT, money("100.00"), "salary");

        assertEquals(fromInt, fromOneDecimal);
        assertEquals(fromOneDecimal, fromTwoDecimals);
    }

    @Test
    void differentAmount_producesDifferentFingerprint() {
        String a = IdempotencyFingerprint.of("acc-1", TransactionType.DEPOSIT, money("100.00"), "salary");
        String b = IdempotencyFingerprint.of("acc-1", TransactionType.DEPOSIT, money("200.00"), "salary");

        assertNotEquals(a, b);
    }

    @Test
    void differentDescription_producesDifferentFingerprint() {
        String a = IdempotencyFingerprint.of("acc-1", TransactionType.DEPOSIT, money("100.00"), "salary");
        String b = IdempotencyFingerprint.of("acc-1", TransactionType.DEPOSIT, money("100.00"), "bonus");

        assertNotEquals(a, b);
    }

    @Test
    void differentAccount_producesDifferentFingerprint() {
        String a = IdempotencyFingerprint.of("acc-1", TransactionType.DEPOSIT, money("100.00"), "salary");
        String b = IdempotencyFingerprint.of("acc-2", TransactionType.DEPOSIT, money("100.00"), "salary");

        assertNotEquals(a, b);
    }

    @Test
    void differentOperationType_producesDifferentFingerprint() {
        String deposit = IdempotencyFingerprint.of("acc-1", TransactionType.DEPOSIT, money("100.00"), "x");
        String withdrawal = IdempotencyFingerprint.of("acc-1", TransactionType.WITHDRAWAL, money("100.00"), "x");

        assertNotEquals(deposit, withdrawal);
    }

    @Test
    void nullDescription_treatedAsEmpty() {
        String nullDesc = IdempotencyFingerprint.of("acc-1", TransactionType.DEPOSIT, money("100.00"), null);
        String emptyDesc = IdempotencyFingerprint.of("acc-1", TransactionType.DEPOSIT, money("100.00"), "");

        assertEquals(nullDesc, emptyDesc);
    }

    @Test
    void nullField_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> IdempotencyFingerprint.of(null, TransactionType.DEPOSIT, money("100.00"), "x"));
        assertThrows(IllegalArgumentException.class,
                () -> IdempotencyFingerprint.of("acc-1", null, money("100.00"), "x"));
        assertThrows(IllegalArgumentException.class,
                () -> IdempotencyFingerprint.of("acc-1", TransactionType.DEPOSIT, null, "x"));
    }
}
