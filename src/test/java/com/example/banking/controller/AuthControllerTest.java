package com.example.banking.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.banking.dto.RegisterRequest;
import com.example.banking.dto.UserResponse;
import com.example.banking.exception.DuplicateEmailException;
import com.example.banking.exception.GlobalExceptionHandler;
import com.example.banking.exception.InvalidCredentialsException;
import com.example.banking.security.JwtService;
import com.example.banking.service.UserService;

// Web-layer test: real controller + real validation + real exception handler,
// fake (mocked) service and JWT service. No Spring context and no MongoDB needed.
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String REGISTER_URL = "/api/auth/register";
    private static final String LOGIN_URL = "/api/auth/login";

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(userService, jwtService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---------- /register (unchanged behaviour, still fully covered) ----------

    @Test
    void register_validRequest_returns201_withoutPassword() throws Exception {
        when(userService.register(any(RegisterRequest.class)))
                .thenReturn(new UserResponse("1", "Ankit", "ankit@example.com", Instant.now()));

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Ankit","email":"ankit@example.com","password":"SecurePassword123"}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("Ankit"))
                .andExpect(jsonPath("$.email").value("ankit@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString("SecurePassword123"))));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateEmailException("ankit@example.com"));

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Ankit","email":"ankit@example.com","password":"SecurePassword123"}
                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Ankit","email":"not-an-email","password":"SecurePassword123"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void register_missingFields_returns400_withAllFieldErrors() throws Exception {
        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void register_weakPassword_returns400() throws Exception {
        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Ankit","email":"ankit@example.com","password":"short"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void register_malformedJson_returns400() throws Exception {
        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content("{ this is not json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void register_unexpectedError_returns500_withoutLeakingDetails() throws Exception {
        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("secret internal detail"));

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Ankit","email":"ankit@example.com","password":"SecurePassword123"}
                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("secret internal detail"))));
    }

    // ---------- /login (new) ----------

    @Test
    void login_validCredentials_returns200_withTokenAndUser() throws Exception {
        when(userService.verifyCredentials("ankit@example.com", "SecurePassword123"))
                .thenReturn(new UserResponse("1", "Ankit", "ankit@example.com", Instant.now()));
        when(jwtService.generateToken("1", "ankit@example.com")).thenReturn("fake.jwt.token");

        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"ankit@example.com","password":"SecurePassword123"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake.jwt.token"))
                .andExpect(jsonPath("$.user.id").value("1"))
                .andExpect(jsonPath("$.user.name").value("Ankit"))
                .andExpect(jsonPath("$.user.email").value("ankit@example.com"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString("SecurePassword123"))));
    }

    @Test
    void login_wrongPassword_returns401_withGenericMessage() throws Exception {
        when(userService.verifyCredentials(anyString(), anyString()))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"ankit@example.com","password":"WrongPassword999"}
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        verifyNoInteractions(jwtService);
    }

    @Test
    void login_unknownEmail_returns401_withTheExactSameMessageAsWrongPassword() throws Exception {
        // Proves the endpoint does not leak whether an email is registered: both failure
        // cases must produce byte-for-byte the same response body shape and message.
        when(userService.verifyCredentials(anyString(), anyString()))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"ghost@example.com","password":"SecurePassword123"}
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        verifyNoInteractions(jwtService);
    }

    @Test
    void login_missingEmail_returns400() throws Exception {
        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content("""
                {"password":"SecurePassword123"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());

        verifyNoInteractions(userService, jwtService);
    }

    @Test
    void login_missingPassword_returns400() throws Exception {
        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"ankit@example.com"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());

        verifyNoInteractions(userService, jwtService);
    }

    @Test
    void login_malformedJson_returns400() throws Exception {
        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content("{ not json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService, jwtService);
    }
}