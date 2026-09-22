package com.example.banking.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.banking.exception.InvalidTokenException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;

/**
 * Issues and validates JWTs (HS256). This class only knows about tokens: it does not
 * know about HTTP, does not touch the database, and is not wired into any controller yet
 * (that happens in Step 3 - login endpoint - and Step 4 - security filter chain).
 *
 * WHAT is a JWT here: a signed, self-contained string carrying two claims - userId and
 * email - plus an expiry time. WHY signed: anyone can read a JWT's contents (it is only
 * Base64, not encrypted), but only someone holding jwt.secret can produce a signature
 * that this class will accept, so a client cannot forge or edit a token undetected.
 */
@Component
public class JwtService {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_EMAIL = "email";

    // RFC 7518 requires an HMAC-SHA256 key to be at least 256 bits (32 bytes) long.
    private static final int MIN_KEY_BYTES = 32;

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.signingKey = buildSigningKey(secret);
        if (expirationMs <= 0) {
            throw new IllegalStateException("jwt.expiration-ms must be a positive number of milliseconds");
        }
        this.expirationMs = expirationMs;
    }

    private static SecretKey buildSigningKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret is not configured. Set the JWT_SECRET environment variable to a "
                            + "Base64-encoded value that decodes to at least 32 bytes (256 bits). "
                            + "You can generate one locally with: openssl rand -base64 32");
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (DecodingException | IllegalArgumentException ex) {
            throw new IllegalStateException("JWT_SECRET must be a valid Base64-encoded string", ex);
        }

        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET is too short: the decoded key must be at least 32 bytes (256 bits) "
                            + "for the HS256 algorithm, but was " + keyBytes.length + " bytes");
        }

        try {
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (WeakKeyException ex) {
            throw new IllegalStateException("JWT_SECRET produces a key that is too weak for HS256", ex);
        }
    }

    /** Generates a signed token carrying the user's id and email, valid for jwt.expiration-ms. */
    public String generateToken(String userId, String email) {
        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + expirationMs);

        return Jwts.builder()
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_EMAIL, email)
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(signingKey)
                .compact();
    }

    /** @throws InvalidTokenException if the token is missing, malformed, tampered with, or expired */
    public String extractUserId(String token) {
        return parseClaims(token).get(CLAIM_USER_ID, String.class);
    }

    /** @throws InvalidTokenException if the token is missing, malformed, tampered with, or expired */
    public String extractEmail(String token) {
        return parseClaims(token).get(CLAIM_EMAIL, String.class);
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new InvalidTokenException("Token has expired", ex);
        } catch (JwtException | IllegalArgumentException ex) {
            // Covers: bad signature (tampered), malformed structure, unsupported algorithm,
            // null/blank token, and any other reason the token cannot be trusted.
            throw new InvalidTokenException("Token is invalid", ex);
        }
    }
}
