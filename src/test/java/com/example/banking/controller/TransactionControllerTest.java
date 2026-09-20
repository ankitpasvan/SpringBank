package com.example.banking.controller;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.banking.dto.TransactionResponse;
import com.example.banking.exception.GlobalExceptionHandler;
import com.example.banking.exception.InsufficientFundsException;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.model.TransactionType;
import com.example.banking.service.TransactionService;

// Web-layer test: real controller + real validation + real exception handler,
// fake (mocked) service. No Spring context and no MongoDB needed.
@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    private static final String DEPOSIT_URL = "/api/accounts/acc-1/deposit";
    private static final String WITHDRAW_URL = "/api/accounts/acc-1/withdraw";

    @Mock
    private TransactionService transactionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TransactionController(transactionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private TransactionResponse sampleResponse(TransactionType type, String amount, String balanceAfter) {
        return new TransactionResponse("tx-1", "acc-1", type, new BigDecimal(amount),
                new BigDecimal(balanceAfter), "note", Instant.now());
    }

    private String body(String amountJson) {
        return "{\"amount\":" + amountJson + "}";
    }

    @Test
    void deposit_validRequest_returns201() throws Exception {
        when(transactionService.deposit("acc-1", new BigDecimal("500.00"), "salary"))
                .thenReturn(sampleResponse(TransactionType.DEPOSIT, "500.00", "1500.00"));

        mockMvc.perform(post(DEPOSIT_URL).contentType(MediaType.APPLICATION_JSON).content("""
                {"amount":500.00,"description":"salary"}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("tx-1"))
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.accountId").value("acc-1"))
                .andExpect(jsonPath("$.amount").exists())
                .andExpect(jsonPath("$.balanceAfter").exists());
    }

    @Test
    void withdraw_validRequest_returns201() throws Exception {
        when(transactionService.withdraw(eq("acc-1"), any(BigDecimal.class), any()))
                .thenReturn(sampleResponse(TransactionType.WITHDRAWAL, "200.00", "800.00"));

        mockMvc.perform(post(WITHDRAW_URL).contentType(MediaType.APPLICATION_JSON).content(body("200.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"));
    }

    @Test
    void deposit_missingAmount_returns400() throws Exception {
        mockMvc.perform(post(DEPOSIT_URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());

        verifyNoInteractions(transactionService);
    }

    @Test
    void deposit_zeroAmount_returns400() throws Exception {
        mockMvc.perform(post(DEPOSIT_URL).contentType(MediaType.APPLICATION_JSON).content(body("0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());

        verifyNoInteractions(transactionService);
    }

    @Test
    void deposit_negativeAmount_returns400() throws Exception {
        mockMvc.perform(post(DEPOSIT_URL).contentType(MediaType.APPLICATION_JSON).content(body("-5.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());

        verifyNoInteractions(transactionService);
    }

    @Test
    void withdraw_moreThanTwoDecimals_returns400() throws Exception {
        mockMvc.perform(post(WITHDRAW_URL).contentType(MediaType.APPLICATION_JSON).content(body("10.005")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());

        verifyNoInteractions(transactionService);
    }

    @Test
    void deposit_amountAboveLimit_returns400() throws Exception {
        mockMvc.perform(post(DEPOSIT_URL).contentType(MediaType.APPLICATION_JSON).content(body("1000000.01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());

        verifyNoInteractions(transactionService);
    }

    @Test
    void deposit_amountIsNotANumber_returns400() throws Exception {
        mockMvc.perform(post(DEPOSIT_URL).contentType(MediaType.APPLICATION_JSON).content(body("\"abc\"")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    @Test
    void deposit_descriptionTooLong_returns400() throws Exception {
        String longText = "x".repeat(101);

        mockMvc.perform(post(DEPOSIT_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00,\"description\":\"" + longText + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.description").exists());

        verifyNoInteractions(transactionService);
    }

    @Test
    void withdraw_insufficientFunds_returns422() throws Exception {
        when(transactionService.withdraw(eq("acc-1"), any(BigDecimal.class), any()))
                .thenThrow(new InsufficientFundsException());

        mockMvc.perform(post(WITHDRAW_URL).contentType(MediaType.APPLICATION_JSON).content(body("999.00")))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Insufficient funds for this withdrawal"));
    }

    @Test
    void deposit_accountNotFound_returns404() throws Exception {
        when(transactionService.deposit(eq("acc-1"), any(BigDecimal.class), any()))
                .thenThrow(new ResourceNotFoundException("Account not found with id: acc-1"));

        mockMvc.perform(post(DEPOSIT_URL).contentType(MediaType.APPLICATION_JSON).content(body("10.00")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found with id: acc-1"));
    }
}
