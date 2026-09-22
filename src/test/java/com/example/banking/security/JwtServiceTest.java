package com.example.banking.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.example.banking.exception.InvalidTokenException;

// No mocks anywhere here: every test uses a REAL JwtService with REAL jjwt token
// generation and parsing, because the whole point of this class is cryptographic
// correctness, which a mock cannot verify.
class JwtServiceTest {

    private static final String USER_ID = "user-123";
    private static final String EMAIL = "ankit@example.com";
    private static final long ONE_HOUR_MS = 3_600_000L;

    // A fresh random 256-bit (32 byte) key, Base64-encoded, for each test run.
    private static String randomSecret() {
        byte[] keyBytes = new byte[32];
        new java.security.SecureRandom().nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    private JwtService newService(long expirationMs) {
        return new JwtService(randomSecret(), expirationMs);
    }

    @Test
    void generateToken_producesANonEmptyThreePartJwt() {
        JwtService jwtService = newService(ONE_HOUR_MS);

        String token = jwtService.generateToken(USER_ID, EMAIL);

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length, "a JWT has 3 dot-separated parts: header.payload.signature");
    }

    @Test
    void generateToken_thenExtractUserId_returnsTheSameUserId() {
        JwtService jwtService = newService(ONE_HOUR_MS);

        String token = jwtService.generateToken(USER_ID, EMAIL);

        assertEquals(USER_ID, jwtService.extractUserId(token));
    }

    @Test
    void generateToken_thenExtractEmail_returnsTheSameEmail() {
        JwtService jwtService = newService(ONE_HOUR_MS);

        String token = jwtService.generateToken(USER_ID, EMAIL);

        assertEquals(EMAIL, jwtService.extractEmail(token));
    }

    @Test
    void roundTrip_bothClaimsSurviveTogetherOnTheSameToken() {
        JwtService jwtService = newService(ONE_HOUR_MS);

        String token = jwtService.generateToken(USER_ID, EMAIL);

        assertEquals(USER_ID, jwtService.extractUserId(token));
        assertEquals(EMAIL, jwtService.extractEmail(token));
    }

    @Test
    void expiredToken_isRejected() throws InterruptedException {
        // 1 millisecond expiry: the token is already expired by the time we check it.
        JwtService jwtService = newService(1L);
        String token = jwtService.generateToken(USER_ID, EMAIL);

        Thread.sleep(50);

        InvalidTokenException ex = assertThrows(InvalidTokenException.class,
                () -> jwtService.extractUserId(token));
        assertTrue(ex.getMessage().toLowerCase().contains("expired"));
    }

    @Test
    void tamperedToken_isRejected() {
        JwtService jwtService = newService(ONE_HOUR_MS);
        String token = jwtService.generateToken(USER_ID, EMAIL);

        // Flip the last character of the payload segment, which changes the claims
        // without touching the signature -> the signature no longer matches.
        String[] parts = token.split("\\.");
        char lastChar = parts[1].charAt(parts[1].length() - 1);
        char replacement = lastChar == 'A' ? 'B' : 'A';
        String tamperedPayload = parts[1].substring(0, parts[1].length() - 1) + replacement;
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertThrows(InvalidTokenException.class, () -> jwtService.extractUserId(tamperedToken));
    }

    @Test
    void tokenSignedWithADifferentSecret_isRejected() {
        JwtService issuer = newService(ONE_HOUR_MS);
        JwtService verifier = newService(ONE_HOUR_MS); // different random secret

        String token = issuer.generateToken(USER_ID, EMAIL);

        assertThrows(InvalidTokenException.class, () -> verifier.extractUserId(token));
    }

    @Test
    void garbageString_isRejectedAsInvalid() {
        JwtService jwtService = newService(ONE_HOUR_MS);

        assertThrows(InvalidTokenException.class, () -> jwtService.extractUserId("not-a-jwt-at-all"));
    }

    @Test
    void emptyToken_isRejectedAsInvalid() {
        JwtService jwtService = newService(ONE_HOUR_MS);

        assertThrows(InvalidTokenException.class, () -> jwtService.extractUserId(""));
    }

    @Test
    void missingSecret_failsFastWithClearMessage() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new JwtService("", ONE_HOUR_MS));

        assertTrue(ex.getMessage().contains("JWT_SECRET"));
    }

    @Test
    void blankSecret_failsFastWithClearMessage() {
        assertThrows(IllegalStateException.class, () -> new JwtService("   ", ONE_HOUR_MS));
    }

    @Test
    void secretTooShortFor256Bits_failsFast() {
        // 16 bytes = 128 bits, below the 256-bit minimum required for HS256.
        String shortSecret = Base64.getEncoder().encodeToString(new byte[16]);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new JwtService(shortSecret, ONE_HOUR_MS));

        assertTrue(ex.getMessage().contains("32 bytes"));
    }

    @Test
    void secretThatIsNotValidBase64_failsFast() {
        assertThrows(IllegalStateException.class,
                () -> new JwtService("not valid base64 !!!", ONE_HOUR_MS));
    }

    @Test
    void zeroOrNegativeExpiration_failsFast() {
        String secret = randomSecret();

        assertThrows(IllegalStateException.class, () -> new JwtService(secret, 0L));
        assertThrows(IllegalStateException.class, () -> new JwtService(secret, -1L));
    }
}
