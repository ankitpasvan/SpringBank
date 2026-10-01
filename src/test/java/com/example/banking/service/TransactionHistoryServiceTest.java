package com.example.banking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.banking.dto.PageResponse;
import com.example.banking.dto.TransactionResponse;
import com.example.banking.exception.InvalidRequestException;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.model.Transaction;
import com.example.banking.model.TransactionType;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.TransactionSearchRepository;

@ExtendWith(MockitoExtension.class)
class TransactionHistoryServiceTest {

    private static final String ACCOUNT_ID = "acc-1";
    private static final String USER_ID = "user-1";

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionSearchRepository transactionSearchRepository;

    private TransactionHistoryService historyService;

    @BeforeEach
    void setUp() {
        historyService = new TransactionHistoryService(accountRepository, transactionSearchRepository);
    }

    private Transaction sampleTransaction() {
        return new Transaction(ACCOUNT_ID, TransactionType.DEPOSIT, new BigDecimal("100.00"),
                new BigDecimal("100.00"), "note");
    }

    @Test
    void getHistory_ownerMatches_returnsMappedPageResponse() {
        when(accountRepository.existsByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(true);
        Page<Transaction> page = new PageImpl<>(List.of(sampleTransaction()), PageRequest.of(0, 20), 1);
        when(transactionSearchRepository.search(eq(ACCOUNT_ID), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<TransactionResponse> response =
                historyService.getHistory(ACCOUNT_ID, USER_ID, null, null, null, 0, 20);

        assertEquals(1, response.content().size());
        assertEquals(0, response.page());
        assertEquals(20, response.size());
        assertEquals(1, response.totalElements());
        assertEquals(1, response.totalPages());
        assertEquals(TransactionType.DEPOSIT, response.content().get(0).type());
    }

    @Test
    void getHistory_differentOwner_throwsResourceNotFound() {
        // Account exists but existsByIdAndUserId is false for this caller - identical to
        // "account doesn't exist" from this method's point of view, so account ids can't
        // be enumerated by their HTTP status.
        when(accountRepository.existsByIdAndUserId(ACCOUNT_ID, "someone-else")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> historyService.getHistory(ACCOUNT_ID, "someone-else", null, null, null, 0, 20));
    }

    @Test
    void getHistory_accountNotFound_throwsResourceNotFound() {
        when(accountRepository.existsByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> historyService.getHistory(ACCOUNT_ID, USER_ID, null, null, null, 0, 20));
    }

    @Test
    void getHistory_negativePage_throwsInvalidRequest() {
        assertThrows(InvalidRequestException.class,
                () -> historyService.getHistory(ACCOUNT_ID, USER_ID, null, null, null, -1, 20));
    }

    @Test
    void getHistory_sizeZero_throwsInvalidRequest() {
        assertThrows(InvalidRequestException.class,
                () -> historyService.getHistory(ACCOUNT_ID, USER_ID, null, null, null, 0, 0));
    }

    @Test
    void getHistory_sizeAboveMax_throwsInvalidRequest() {
        assertThrows(InvalidRequestException.class,
                () -> historyService.getHistory(ACCOUNT_ID, USER_ID, null, null, null, 0,
                        TransactionHistoryService.MAX_PAGE_SIZE + 1));
    }

    @Test
    void getHistory_fromAfterTo_throwsInvalidRequest() {
        LocalDate from = LocalDate.of(2026, 2, 1);
        LocalDate to = LocalDate.of(2026, 1, 1);

        assertThrows(InvalidRequestException.class,
                () -> historyService.getHistory(ACCOUNT_ID, USER_ID, null, from, to, 0, 20));
    }

    @Test
    void getHistory_sortsNewestFirst() {
        when(accountRepository.existsByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(true);
        when(transactionSearchRepository.search(eq(ACCOUNT_ID), isNull(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        historyService.getHistory(ACCOUNT_ID, USER_ID, null, LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31), 0, 20);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionSearchRepository).search(eq(ACCOUNT_ID), isNull(), any(), any(), pageableCaptor.capture());
        Pageable used = pageableCaptor.getValue();
        assertEquals(org.springframework.data.domain.Sort.Direction.DESC,
                used.getSort().getOrderFor("createdAt").getDirection());
    }
}