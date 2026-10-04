package com.example.banking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.banking.dto.TransferResponse;
import com.example.banking.exception.GlobalExceptionHandler;
import com.example.banking.exception.InsufficientFundsException;
import com.example.banking.exception.InvalidRequestException;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.service.TransferService;

// Web-layer test: real controller + real validation + real exception handler,
// fake (mocked) service. No Spring context and no MongoDB needed.
@ExtendWith(MockitoExtension.class)
class TransferControllerTest {

    private static final String URL = "/api/accounts/acc-1/transfer";

    @Mock
    private TransferService transferService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TransferController(transferService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // Principal is a single-method interface (getName()), so a lambda is enough here -
    // no need to pull in Spring Security types just to fake "who is calling".
    private Principal principal(String userId) {
        return () -> userId;
    }

    private TransferResponse sampleResponse() {
        return new TransferResponse("tx-1", "acc-1", "acc-2", new BigDecimal("100.00"),
                new BigDecimal("400.00"), "rent split", Instant.now());
    }

    @Test
    void transfer_validRequest_returns201_andForwardsAuthenticatedUserIdToService() throws Exception {
        when(transferService.transfer("acc-1", "user-1", "acc-2", new BigDecimal("100.00"), "rent split"))
                .thenReturn(sampleResponse());

        mockMvc.perform(post(URL).principal(principal("user-1"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                {"toAccountId":"acc-2","amount":100.00,"description":"rent split"}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("tx-1"))
                .andExpect(jsonPath("$.fromAccountId").value("acc-1"))
                .andExpect(jsonPath("$.toAccountId").value("acc-2"))
                .andExpect(jsonPath("$.amount").exists())
                .andExpect(jsonPath("$.senderBalanceAfter").exists());
    }

    @Test
    void transfer_missingToAccountId_returns400() throws Exception {
        mockMvc.perform(post(URL).principal(principal("user-1"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                {"amount":100.00}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.toAccountId").exists());

        verifyNoInteractions(transferService);
    }

    @Test
    void transfer_missingAmount_returns400() throws Exception {
        mockMvc.perform(post(URL).principal(principal("user-1"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                {"toAccountId":"acc-2"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());

        verifyNoInteractions(transferService);
    }

    @Test
    void transfer_negativeAmount_returns400() throws Exception {
        mockMvc.perform(post(URL).principal(principal("user-1"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                {"toAccountId":"acc-2","amount":-10.00}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());

        verifyNoInteractions(transferService);
    }

    @Test
    void transfer_selfTransfer_returns400() throws Exception {
        // The controller forwards whatever toAccountId the client sent - it is
        // TransferService that detects "same account" and rejects it.
        when(transferService.transfer(eq("acc-1"), eq("user-1"), eq("acc-1"), any(BigDecimal.class), any()))
                .thenThrow(new InvalidRequestException("Cannot transfer to the same account"));

        mockMvc.perform(post(URL).principal(principal("user-1"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                {"toAccountId":"acc-1","amount":100.00}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot transfer to the same account"));
    }

    @Test
    void transfer_senderNotOwner_returns404() throws Exception {
        // "someone-else" is authenticated (a valid token), but does not own "acc-1" - the
        // controller still forwards their own true id, and the service reports "not found".
        when(transferService.transfer(eq("acc-1"), eq("someone-else"), eq("acc-2"), any(BigDecimal.class), any()))
                .thenThrow(new ResourceNotFoundException("Account not found with id: acc-1"));

        mockMvc.perform(post(URL).principal(principal("someone-else"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                {"toAccountId":"acc-2","amount":100.00}
                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found with id: acc-1"));
    }

    @Test
    void transfer_receiverNotFound_returns404() throws Exception {
        when(transferService.transfer(eq("acc-1"), eq("user-1"), eq("ghost"), any(BigDecimal.class), any()))
                .thenThrow(new ResourceNotFoundException("Account not found with id: ghost"));

        mockMvc.perform(post(URL).principal(principal("user-1"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                {"toAccountId":"ghost","amount":100.00}
                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found with id: ghost"));
    }

    @Test
    void transfer_insufficientFunds_returns422() throws Exception {
        when(transferService.transfer(eq("acc-1"), eq("user-1"), eq("acc-2"), any(BigDecimal.class), any()))
                .thenThrow(new InsufficientFundsException());

        mockMvc.perform(post(URL).principal(principal("user-1"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                {"toAccountId":"acc-2","amount":999999.00}
                """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Insufficient funds for this withdrawal"));
    }
}