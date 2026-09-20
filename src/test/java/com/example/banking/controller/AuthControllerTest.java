package com.example.banking.controller;


import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
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
import com.example.banking.service.UserService;

// Web-layer test: real controller + real validation + real exception handler,
// fake (mocked) service. No Spring context and no MongoDB needed.
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String URL = "/api/auth/register";

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void register_validRequest_returns201_withoutPassword() throws Exception {
        when(userService.register(any(RegisterRequest.class)))
                .thenReturn(new UserResponse("1", "Ankit", "ankit@example.com", Instant.now()));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
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

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Ankit","email":"ankit@example.com","password":"SecurePassword123"}
                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Ankit","email":"not-an-email","password":"SecurePassword123"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void register_missingFields_returns400_withAllFieldErrors() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void register_weakPassword_returns400() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Ankit","email":"ankit@example.com","password":"short"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void register_malformedJson_returns400() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{ this is not json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void register_unexpectedError_returns500_withoutLeakingDetails() throws Exception {
        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("secret internal detail"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Ankit","email":"ankit@example.com","password":"SecurePassword123"}
                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("secret internal detail"))));
    }
}
