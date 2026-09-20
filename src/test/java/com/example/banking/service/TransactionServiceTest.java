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

import com.example.banking.dto.TransactionResponse;
import com.example.banking.exception.InsufficientFundsException;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.model.Account;
import com.example.banking.model.AccountType;
import com.example.banking.model.Transaction;
import com.example.banking.model.TransactionType;
import com.example.banking.repository.AccountBalanceRepository;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final String ACCOUNT_ID = "acc-1";

    @Mock
    private AccountBalanceRepository accountBalanceRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(accountBalanceRepository, accountRepository,
                transactionRepository);
    }

    private Account accountWithBalance(String balance) {
        return new Account("123456789012", "user-1", AccountType.SAVINGS, new BigDecimal(balance));
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected " + expected + " but was " + actual);
    }

    @Test
    void deposit_success_recordsTransactionWithBalanceAfter() {
        when(accountBalanceRepository.deposit(ACCOUNT_ID, new BigDecimal("500.00")))
                .thenReturn(Optional.of(accountWithBalance("1500.00")));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse response = transactionService.deposit(ACCOUNT_ID, new BigDecimal("500.00"), "salary");

        assertEquals(TransactionType.DEPOSIT, response.type());
        assertEquals(ACCOUNT_ID, response.accountId());
        assertEquals("salary", response.description());
        assertMoney("500.00", response.amount());
        assertMoney("1500.00", response.balanceAfter());
    }

    @Test
    void deposit_amountWithoutDecimals_isNormalizedToTwoDecimals() {
        when(accountBalanceRepository.deposit(ACCOUNT_ID, new BigDecimal("100.00")))
                .thenReturn(Optional.of(accountWithBalance("100.00")));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.deposit(ACCOUNT_ID, new BigDecimal("100"), null);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertEquals(2, captor.getValue().getAmount().scale());
    }

    @Test
    void deposit_accountNotFound_throwsAndRecordsNothing() {
        when(accountBalanceRepository.deposit(ACCOUNT_ID, new BigDecimal("50.00"))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.deposit(ACCOUNT_ID, new BigDecimal("50.00"), null));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void withdraw_success_recordsWithdrawalTransaction() {
        when(accountBalanceRepository.withdraw(ACCOUNT_ID, new BigDecimal("200.00")))
                .thenReturn(Optional.of(accountWithBalance("800.00")));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse response = transactionService.withdraw(ACCOUNT_ID, new BigDecimal("200.00"), "rent");

        assertEquals(TransactionType.WITHDRAWAL, response.type());
        assertMoney("200.00", response.amount());
        assertMoney("800.00", response.balanceAfter());
    }

    @Test
    void withdraw_insufficientFunds_throwsAndRecordsNothing() {
        when(accountBalanceRepository.withdraw(ACCOUNT_ID, new BigDecimal("999.00"))).thenReturn(Optional.empty());
        when(accountRepository.existsById(ACCOUNT_ID)).thenReturn(true);

        assertThrows(InsufficientFundsException.class,
                () -> transactionService.withdraw(ACCOUNT_ID, new BigDecimal("999.00"), null));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void withdraw_accountNotFound_throwsResourceNotFound() {
        when(accountBalanceRepository.withdraw(ACCOUNT_ID, new BigDecimal("10.00"))).thenReturn(Optional.empty());
        when(accountRepository.existsById(ACCOUNT_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.withdraw(ACCOUNT_ID, new BigDecimal("10.00"), null));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void depositAndWithdraw_nonPositiveAmount_isRejectedBeforeTouchingTheDatabase() {
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.deposit(ACCOUNT_ID, BigDecimal.ZERO, null));
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.withdraw(ACCOUNT_ID, new BigDecimal("-5.00"), null));
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.deposit(ACCOUNT_ID, null, null));

        verifyNoInteractions(accountBalanceRepository, accountRepository, transactionRepository);
    }
}
