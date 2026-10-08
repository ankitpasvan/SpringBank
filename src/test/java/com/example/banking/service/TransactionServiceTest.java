package com.example.banking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.banking.dto.TransactionResponse;
import com.example.banking.exception.IdempotencyInProgressException;
import com.example.banking.exception.InsufficientFundsException;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.model.Account;
import com.example.banking.model.AccountType;
import com.example.banking.model.IdempotencyRecord;
import com.example.banking.model.Transaction;
import com.example.banking.model.TransactionType;
import com.example.banking.repository.AccountBalanceRepository;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final String ACCOUNT_ID = "acc-1";
    private static final String USER_ID = "user-1";
    private static final String KEY = "key-abc";

    @Mock
    private AccountBalanceRepository accountBalanceRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private IdempotencyService idempotencyService;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(accountBalanceRepository, accountRepository,
                transactionRepository, idempotencyService);
    }

    private Account accountWithBalance(String balance) {
        return new Account("123456789012", USER_ID, AccountType.SAVINGS, new BigDecimal(balance));
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected " + expected + " but was " + actual);
    }

    private IdempotencyRecord freshClaim(String recordId) {
        IdempotencyRecord claim = new IdempotencyRecord(USER_ID, KEY);
        ReflectionTestUtils.setField(claim, "id", recordId);
        return claim;
    }

    private IdempotencyRecord completedClaim(String resultTransactionId) {
        IdempotencyRecord claim = new IdempotencyRecord(USER_ID, KEY);
        claim.markCompleted(resultTransactionId, null);
        return claim;
    }

    // ---------- existing behaviour, now with a trailing null idempotencyKey (no change) ----------

    @Test
    void deposit_success_recordsTransactionWithBalanceAfter() {
        when(accountBalanceRepository.deposit(ACCOUNT_ID, USER_ID, new BigDecimal("500.00")))
                .thenReturn(Optional.of(accountWithBalance("1500.00")));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse response =
                transactionService.deposit(ACCOUNT_ID, USER_ID, new BigDecimal("500.00"), "salary", null);

        assertEquals(TransactionType.DEPOSIT, response.type());
        assertEquals(ACCOUNT_ID, response.accountId());
        assertEquals("salary", response.description());
        assertMoney("500.00", response.amount());
        assertMoney("1500.00", response.balanceAfter());
        verifyNoInteractions(idempotencyService);
    }

    @Test
    void deposit_amountWithoutDecimals_isNormalizedToTwoDecimals() {
        when(accountBalanceRepository.deposit(ACCOUNT_ID, USER_ID, new BigDecimal("100.00")))
                .thenReturn(Optional.of(accountWithBalance("100.00")));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.deposit(ACCOUNT_ID, USER_ID, new BigDecimal("100"), null, null);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertEquals(2, captor.getValue().getAmount().scale());
    }

    @Test
    void deposit_accountNotFound_throwsAndRecordsNothing() {
        when(accountBalanceRepository.deposit(ACCOUNT_ID, USER_ID, new BigDecimal("50.00")))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.deposit(ACCOUNT_ID, USER_ID, new BigDecimal("50.00"), null, null));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void deposit_differentOwner_throwsResourceNotFound() {
        when(accountBalanceRepository.deposit(ACCOUNT_ID, "someone-else", new BigDecimal("50.00")))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.deposit(ACCOUNT_ID, "someone-else", new BigDecimal("50.00"), null, null));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void withdraw_success_recordsWithdrawalTransaction() {
        when(accountBalanceRepository.withdraw(ACCOUNT_ID, USER_ID, new BigDecimal("200.00")))
                .thenReturn(Optional.of(accountWithBalance("800.00")));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse response =
                transactionService.withdraw(ACCOUNT_ID, USER_ID, new BigDecimal("200.00"), "rent", null);

        assertEquals(TransactionType.WITHDRAWAL, response.type());
        assertMoney("200.00", response.amount());
        assertMoney("800.00", response.balanceAfter());
        verifyNoInteractions(idempotencyService);
    }

    @Test
    void withdraw_insufficientFunds_throwsAndRecordsNothing() {
        when(accountBalanceRepository.withdraw(ACCOUNT_ID, USER_ID, new BigDecimal("999.00")))
                .thenReturn(Optional.empty());
        when(accountRepository.existsByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(true);

        assertThrows(InsufficientFundsException.class,
                () -> transactionService.withdraw(ACCOUNT_ID, USER_ID, new BigDecimal("999.00"), null, null));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void withdraw_accountNotFound_throwsResourceNotFound() {
        when(accountBalanceRepository.withdraw(ACCOUNT_ID, USER_ID, new BigDecimal("10.00")))
                .thenReturn(Optional.empty());
        when(accountRepository.existsByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.withdraw(ACCOUNT_ID, USER_ID, new BigDecimal("10.00"), null, null));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void withdraw_differentOwner_throwsResourceNotFound_notInsufficientFunds() {
        when(accountBalanceRepository.withdraw(ACCOUNT_ID, "someone-else", new BigDecimal("10.00")))
                .thenReturn(Optional.empty());
        when(accountRepository.existsByIdAndUserId(ACCOUNT_ID, "someone-else")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.withdraw(ACCOUNT_ID, "someone-else", new BigDecimal("10.00"), null, null));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void depositAndWithdraw_nonPositiveAmount_isRejectedBeforeTouchingTheDatabase() {
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.deposit(ACCOUNT_ID, USER_ID, BigDecimal.ZERO, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.withdraw(ACCOUNT_ID, USER_ID, new BigDecimal("-5.00"), null, null));
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.deposit(ACCOUNT_ID, USER_ID, null, null, null));

        verifyNoInteractions(accountBalanceRepository, accountRepository, transactionRepository, idempotencyService);
    }

    // ---------- idempotency ----------

    @Test
    void deposit_withCompletedIdempotencyKey_replaysWithoutExecutingAgain() {
        Transaction existing = new Transaction(ACCOUNT_ID, TransactionType.DEPOSIT, new BigDecimal("500.00"),
                new BigDecimal("1500.00"), "salary");
        ReflectionTestUtils.setField(existing, "id", "tx-1");
        when(idempotencyService.claim(USER_ID, KEY)).thenReturn(completedClaim("tx-1"));
        when(transactionRepository.findById("tx-1")).thenReturn(Optional.of(existing));

        TransactionResponse response =
                transactionService.deposit(ACCOUNT_ID, USER_ID, new BigDecimal("500.00"), "salary", KEY);

        assertEquals("tx-1", response.id());
        assertMoney("1500.00", response.balanceAfter());
        verifyNoInteractions(accountBalanceRepository);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void withdraw_withCompletedIdempotencyKey_replaysWithoutExecutingAgain() {
        Transaction existing = new Transaction(ACCOUNT_ID, TransactionType.WITHDRAWAL, new BigDecimal("200.00"),
                new BigDecimal("800.00"), "rent");
        ReflectionTestUtils.setField(existing, "id", "tx-2");
        when(idempotencyService.claim(USER_ID, KEY)).thenReturn(completedClaim("tx-2"));
        when(transactionRepository.findById("tx-2")).thenReturn(Optional.of(existing));

        TransactionResponse response =
                transactionService.withdraw(ACCOUNT_ID, USER_ID, new BigDecimal("200.00"), "rent", KEY);

        assertEquals("tx-2", response.id());
        assertMoney("800.00", response.balanceAfter());
        verify(accountBalanceRepository, never()).withdraw(any(), any(), any());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void deposit_freshIdempotencyKey_executesOnceAndMarksCompleted() {
        when(idempotencyService.claim(USER_ID, KEY)).thenReturn(freshClaim("rec-1"));
        when(accountBalanceRepository.deposit(ACCOUNT_ID, USER_ID, new BigDecimal("500.00")))
                .thenReturn(Optional.of(accountWithBalance("1500.00")));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            ReflectionTestUtils.setField(t, "id", "tx-1");
            return t;
        });

        TransactionResponse response =
                transactionService.deposit(ACCOUNT_ID, USER_ID, new BigDecimal("500.00"), "salary", KEY);

        verify(accountBalanceRepository, org.mockito.Mockito.times(1)).deposit(ACCOUNT_ID, USER_ID,
                new BigDecimal("500.00"));
        verify(idempotencyService).markCompleted("rec-1", "tx-1", null);
        verify(idempotencyService, never()).release(any());
        assertEquals("tx-1", response.id());
    }

    @Test
    void deposit_idempotencyKeyInProgress_propagatesExceptionWithoutTouchingMoney() {
        when(idempotencyService.claim(USER_ID, KEY)).thenThrow(new IdempotencyInProgressException());

        assertThrows(IdempotencyInProgressException.class,
                () -> transactionService.deposit(ACCOUNT_ID, USER_ID, new BigDecimal("10.00"), null, KEY));

        verifyNoInteractions(accountBalanceRepository, transactionRepository);
    }

    @Test
    void deposit_failedBusinessOperation_releasesClaim_andDoesNotMarkCompleted() {
        when(idempotencyService.claim(USER_ID, KEY)).thenReturn(freshClaim("rec-1"));
        when(accountBalanceRepository.deposit(ACCOUNT_ID, USER_ID, new BigDecimal("50.00")))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.deposit(ACCOUNT_ID, USER_ID, new BigDecimal("50.00"), null, KEY));

        verify(idempotencyService).release("rec-1");
        verify(idempotencyService, never()).markCompleted(any(), any(), any());
    }

    @Test
    void withdraw_failedBusinessOperation_releasesClaim_andDoesNotMarkCompleted() {
        when(idempotencyService.claim(USER_ID, KEY)).thenReturn(freshClaim("rec-2"));
        when(accountBalanceRepository.withdraw(ACCOUNT_ID, USER_ID, new BigDecimal("999.00")))
                .thenReturn(Optional.empty());
        when(accountRepository.existsByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(true);

        assertThrows(InsufficientFundsException.class,
                () -> transactionService.withdraw(ACCOUNT_ID, USER_ID, new BigDecimal("999.00"), null, KEY));

        verify(idempotencyService).release("rec-2");
        verify(idempotencyService, never()).markCompleted(any(), any(), any());
    }
}