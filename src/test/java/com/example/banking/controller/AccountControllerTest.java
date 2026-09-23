package com.example.banking.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.banking.dto.AccountResponse;
import com.example.banking.exception.GlobalExceptionHandler;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.model.AccountType;
import com.example.banking.service.AccountService;

// Web-layer test: real controller + real validation + real exception handler,
// fake (mocked) service. No Spring context and no MongoDB needed.
@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    private static final String URL = "/api/accounts";

    @Mock
    private AccountService accountService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AccountController(accountService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // Principal is a single-method interface (getName()), so a lambda is enough here -
    // no need to pull in Spring Security types just to fake "who is calling".
    private Principal principal(String userId) {
        return () -> userId;
    }

    private AccountResponse sampleAccount(String id, String accountNumber, AccountType type) {
        Instant now = Instant.now();
        return new AccountResponse(id, accountNumber, "user-1", type, new BigDecimal("0.00"), now, now);
    }

    @Test
    void createAccount_validRequest_returns201() throws Exception {
        when(accountService.createAccount("user-1", AccountType.SAVINGS))
                .thenReturn(sampleAccount("acc-1", "123456789012", AccountType.SAVINGS));

        mockMvc.perform(post(URL).principal(principal("user-1"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                {"accountType":"SAVINGS"}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("acc-1"))
                .andExpect(jsonPath("$.accountNumber").value("123456789012"))
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.accountType").value("SAVINGS"))
                .andExpect(jsonPath("$.balance").exists());
    }

    @Test
    void getAccountById_returns200() throws Exception {
        when(accountService.getById("acc-1"))
                .thenReturn(sampleAccount("acc-1", "123456789012", AccountType.CURRENT));

        mockMvc.perform(get(URL + "/acc-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("acc-1"))
                .andExpect(jsonPath("$.accountType").value("CURRENT"));
    }

    @Test
    void getAccountsByUserId_returns200_withList() throws Exception {
        when(accountService.getByUserId("user-1")).thenReturn(List.of(
                sampleAccount("acc-1", "111111111111", AccountType.SAVINGS),
                sampleAccount("acc-2", "222222222222", AccountType.CURRENT)));

        mockMvc.perform(get(URL).param("userId", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].accountNumber").value("111111111111"))
                .andExpect(jsonPath("$[1].accountNumber").value("222222222222"));
    }

    @Test
    void createAccount_missingAccountType_returns400() throws Exception {
        mockMvc.perform(post(URL).principal(principal("user-1"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.accountType").exists());

        verifyNoInteractions(accountService);
    }

    @Test
    void createAccount_unknownAccountType_returns400() throws Exception {
        mockMvc.perform(post(URL).principal(principal("user-1"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                {"accountType":"FOO"}
                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(accountService);
    }

    @Test
    void getAccountsByUserId_missingUserIdParam_returns400() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(accountService);
    }

    @Test
    void createAccount_userNotFound_returns404() throws Exception {
        // Even a valid JWT's userId claim can point to a user that no longer exists
        // (e.g. deleted after the token was issued but before it expired).
        when(accountService.createAccount("ghost", AccountType.SAVINGS))
                .thenThrow(new ResourceNotFoundException("User not found with id: ghost"));

        mockMvc.perform(post(URL).principal(principal("ghost"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                {"accountType":"SAVINGS"}
                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found with id: ghost"));
    }

    @Test
    void getAccountById_notFound_returns404() throws Exception {
        when(accountService.getById("missing"))
                .thenThrow(new ResourceNotFoundException("Account not found with id: missing"));

        mockMvc.perform(get(URL + "/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found with id: missing"));
    }
}