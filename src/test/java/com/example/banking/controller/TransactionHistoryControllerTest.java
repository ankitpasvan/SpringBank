package com.example.banking.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.banking.dto.PageResponse;
import com.example.banking.dto.TransactionResponse;
import com.example.banking.exception.GlobalExceptionHandler;
import com.example.banking.exception.InvalidRequestException;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.model.TransactionType;
import com.example.banking.service.TransactionHistoryService;

// Web-layer test: real controller + real exception handler, fake (mocked) service.
@ExtendWith(MockitoExtension.class)
class TransactionHistoryControllerTest {

    private static final String URL = "/api/accounts/acc-1/transactions";

    @Mock
    private TransactionHistoryService transactionHistoryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TransactionHistoryController(transactionHistoryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private TransactionResponse sampleTx(TransactionType type) {
        return new TransactionResponse("tx-1", "acc-1", type, new BigDecimal("100.00"),
                new BigDecimal("100.00"), "note", java.time.Instant.now());
    }

    @Test
    void getHistory_noFilters_returns200WithPagination() throws Exception {
        PageResponse<TransactionResponse> page = new PageResponse<>(
                List.of(sampleTx(TransactionType.DEPOSIT), sampleTx(TransactionType.WITHDRAWAL)), 0, 20, 2, 1);
        when(transactionHistoryService.getHistory(eq("acc-1"), isNull(), isNull(), isNull(), eq(0), eq(20)))
                .thenReturn(page);

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getHistory_withTypeFilter_passesTypeToService() throws Exception {
        when(transactionHistoryService.getHistory(eq("acc-1"), eq(TransactionType.DEPOSIT), isNull(), isNull(),
                eq(0), eq(20)))
                .thenReturn(new PageResponse<>(List.of(sampleTx(TransactionType.DEPOSIT)), 0, 20, 1, 1));

        mockMvc.perform(get(URL).param("type", "DEPOSIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].type").value("DEPOSIT"));
    }

    @Test
    void getHistory_withDateRangeAndPaging_passesValuesToService() throws Exception {
        when(transactionHistoryService.getHistory(eq("acc-1"), isNull(),
                eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 1, 31)), eq(1), eq(5)))
                .thenReturn(new PageResponse<>(List.of(), 1, 5, 0, 0));

        mockMvc.perform(get(URL)
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void getHistory_invalidTypeValue_returns400() throws Exception {
        mockMvc.perform(get(URL).param("type", "NOT_A_TYPE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHistory_invalidDateValue_returns400() throws Exception {
        mockMvc.perform(get(URL).param("from", "not-a-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHistory_invalidPageSize_returns400() throws Exception {
        when(transactionHistoryService.getHistory(eq("acc-1"), isNull(), isNull(), isNull(), eq(0), eq(500)))
                .thenThrow(new InvalidRequestException("Page size must be between 1 and 100"));

        mockMvc.perform(get(URL).param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page size must be between 1 and 100"));
    }

    @Test
    void getHistory_accountNotFound_returns404() throws Exception {
        when(transactionHistoryService.getHistory(eq("acc-1"), isNull(), isNull(), isNull(), eq(0), eq(20)))
                .thenThrow(new ResourceNotFoundException("Account not found with id: acc-1"));

        mockMvc.perform(get(URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found with id: acc-1"));
    }
}
