package com.example.banking.util;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.example.banking.model.TransactionType;

/**
 * Builds a stable SHA-256 fingerprint of an idempotent money request, so that reusing an
 * idempotency key with a DIFFERENT payload can be told apart from a genuine retry.
 *
 * WHY this canonical form: the fingerprint must treat "100", "100.0" and "100.00" as the
 * same amount (callers normalize to scale 2 before calling), while any real difference in
 * account, operation, amount or description must produce a different fingerprint. Fields
 * are length-prefixed so the encoding is unambiguous no matter what the values contain,
 * then hashed so the stored record never keeps raw request details beyond what the
 * transaction row already stores.
 */
public final class IdempotencyFingerprint {

    private IdempotencyFingerprint() {
    }

    /**
     * @param accountId   the account the money moves on (never null)
     * @param type        DEPOSIT or WITHDRAWAL (never null)
     * @param amount      already normalized to scale 2 via the service's normalize()
     * @param description may be null, treated as empty
     * @return lowercase hex SHA-256 of the canonical request representation
     */
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
