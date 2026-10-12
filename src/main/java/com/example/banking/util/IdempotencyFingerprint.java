package com.example.banking.util;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.example.banking.model.TransactionType;


public final class IdempotencyFingerprint {

    private IdempotencyFingerprint() {
    }


    public static String of(String accountId, TransactionType type, BigDecimal amount, String description) {
        if (accountId == null || type == null || amount == null) {
            throw new IllegalArgumentException("Fingerprint fields must not be null");
        }
        String canonical = field(accountId)
                + field(type.name())
                + field(amount.toPlainString())
                + field(description == null ? "" : description);
        return sha256Hex(canonical);
    }


    public static String ofTransfer(String fromAccountId, String toAccountId, BigDecimal amount,
                                    String description) {
        if (fromAccountId == null || toAccountId == null || amount == null) {
            throw new IllegalArgumentException("Fingerprint fields must not be null");
        }
        String canonical = field(fromAccountId)
                + field("TRANSFER")
                + field(toAccountId)
                + field(amount.toPlainString())
                + field(description == null ? "" : description);
        return sha256Hex(canonical);
    }

    private static String field(String value) {
        return value.length() + ":" + value + ";";
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is guaranteed on every Java platform; this is unreachable in practice.
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
