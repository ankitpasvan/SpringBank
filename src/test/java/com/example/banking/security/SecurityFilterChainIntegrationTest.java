package com.example.banking.security;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end test of the REAL Spring Security filter chain wired by SecurityConfig.
 *
 * Unlike the standalone MockMvc used in every other *ControllerTest in this project (which
 * builds a single controller by hand and skips Spring Security entirely), the MockMvc here
 * is built from the FULL Spring context via @AutoConfigureMockMvc, so requests actually pass
 * through JwtAuthenticationFilter and the authorizeHttpRequests() rules from SecurityConfig -
 * this is the only way to prove that wiring is correct end to end.
 *
 * (Spring Boot 4 no longer auto-configures TestRestTemplate for @SpringBootTest, and it is
 * being deprecated in favour of RestTestClient in 4.2 - @AutoConfigureMockMvc avoids both of
 * those moving parts while still exercising the real filter chain.)
 *
 * Every request body below is deliberately invalid/minimal so Bean Validation rejects it
 * BEFORE any controller reaches MongoDB. MongoDB is not running in this environment, and a
 * real query would hang until the driver's connection timeout instead of failing fast.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "jwt.secret=XzloAJfJIyp5Fr8WTUAtBHO0xC00qWr7Hzumm+cijHw=",
        "spring.data.mongodb.auto-index-creation=false"
})
class SecurityFilterChainIntegrationTest {

    private static final String TEST_SECRET = "XzloAJfJIyp5Fr8WTUAtBHO0xC00qWr7Hzumm+cijHw=";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicRegisterEndpoint_isReachableWithoutAnyToken() throws Exception {
        // {} fails validation (name/email/password required) -> 400, proving the request
        // reached the controller instead of being blocked at 401/403 by security.
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publicLoginEndpoint_isReachableWithoutAnyToken() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpoint_withNoToken_returns401() throws Exception {
        mockMvc.perform(get("/api/accounts/000000000000000000000000"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Authentication is required")));
    }

    @Test
    void protectedEndpoint_withGarbageToken_returns401() throws Exception {
        mockMvc.perform(get("/api/accounts/000000000000000000000000")
                        .header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Token is invalid")));
    }

    @Test
    void protectedEndpoint_withExpiredToken_returns401() throws Exception {
        JwtService shortLivedIssuer = new JwtService(TEST_SECRET, 1L);
        String expiredToken = shortLivedIssuer.generateToken("user-123", "ankit@example.com");
        Thread.sleep(50);

        mockMvc.perform(get("/api/accounts/000000000000000000000000")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Token has expired")));
    }

    @Test
    void protectedEndpoint_withValidToken_isAuthenticated_andReachesController() throws Exception {
        JwtService issuer = new JwtService(TEST_SECRET, 3_600_000L);
        String token = issuer.generateToken("user-123", "ankit@example.com");

        // {} fails CreateAccountRequest validation (userId/accountType required) -> 400.
        // Getting 400 (not 401/403) PROVES the token authenticated successfully and the
        // request reached the controller's validation layer, without ever touching MongoDB.
        mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}