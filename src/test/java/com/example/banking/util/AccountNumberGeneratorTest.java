package com.example.banking.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AccountNumberGeneratorTest {

    private final AccountNumberGenerator generator = new AccountNumberGenerator();

    @Test
    void generate_returnsExactly12Digits() {
        for (int i = 0; i < 1000; i++) {
            String number = generator.generate();

            assertEquals(12, number.length());
            assertTrue(number.matches("\\d{12}"), "Only digits allowed but was: " + number);
        }
    }

    @Test
    void generate_neverStartsWithZero() {
        for (int i = 0; i < 1000; i++) {
            assertNotEquals('0', generator.generate().charAt(0));
        }
    }
}
