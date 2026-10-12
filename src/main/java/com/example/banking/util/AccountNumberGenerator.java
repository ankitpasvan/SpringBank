package com.example.banking.util;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;


@Component
public class AccountNumberGenerator {

    private static final int ACCOUNT_NUMBER_LENGTH = 12;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        StringBuilder number = new StringBuilder(ACCOUNT_NUMBER_LENGTH);
        number.append(secureRandom.nextInt(9) + 1); // first digit: 1-9
        for (int i = 1; i < ACCOUNT_NUMBER_LENGTH; i++) {
            number.append(secureRandom.nextInt(10)); // remaining digits: 0-9
        }
        return number.toString();
    }
}
