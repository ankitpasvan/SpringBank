package com.example.banking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.banking.dto.TransferResponse;
import com.example.banking.exception.InsufficientFundsException;
import com.example.banking.exception.InvalidRequestException;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.model.Account;
import com.example.banking.model.AccountType;
import com.example.banking.model.Transaction;
import com.example.banking.model.TransactionType;
import com.example.banking.repository.AccountBalanceRepository;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.TransactionRepository;

// NOTE: @Transactional rollback itself cannot be exercised by a plain unit test (there is
// no Spring container here to intercept the exception and invoke the transaction manager).
// What IS tested is the property that makes rollback meaningful: when a later step fails,
// no transaction record is ever saved - the method must throw before writing anything
// partial. Real rollback (the sender's debit being undone in the database) is exercised by
// @Transactional against PostgreSQL, where it works on any installation (no replica set
// needed, unlike the previous MongoDB implementation).
@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    private static final String FROM_ACCOUNT_ID = "acc-sender";
    private static final String TO_ACCOUNT_ID = "acc-receiver";
    private static final String SENDER_USER_ID = "user-1";
    private static final String RECEIVER_USER_ID = "user-2";

    @Mock
    private AccountBalanceRepository accountBalanceRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private TransferService transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferService(accountBalanceRepository, accountRepository, transactionRepository);
    }

    private Account account(String userId, String balance) {
        return new Account("123456789012", userId, AccountType.SAVINGS, new BigDecimal(balance));
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected " + expected + " but was " + actual);
    }

    @Test
    void transfer_success_debitsSender_creditsReceiver_recordsBothTransactions() {
        when(accountRepository.findById(TO_ACCOUNT_ID))
                .thenReturn(Optional.of(account(RECEIVER_USER_ID, "50.00")));
        when(accountBalanceRepository.withdraw(FROM_ACCOUNT_ID, SENDER_USER_ID, new BigDecimal("100.00")))
                .thenReturn(Optional.of(account(SENDER_USER_ID, "400.00")));
        when(accountBalanceRepository.deposit(TO_ACCOUNT_ID, RECEIVER_USER_ID, new BigDecimal("100.00")))
                .thenReturn(Optional.of(account(RECEIVER_USER_ID, "150.00")));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransferResponse response = transferService.transfer(FROM_ACCOUNT_ID, SENDER_USER_ID, TO_ACCOUNT_ID,
                new BigDecimal("100.00"), "rent split");

        assertEquals(FROM_ACCOUNT_ID, response.fromAccountId());
        assertEquals(TO_ACCOUNT_ID, response.toAccountId());
        assertMoney("100.00", response.amount());
        assertMoney("400.00", response.senderBalanceAfter());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());
        Transaction outgoing = captor.getAllValues().get(0);
        Transaction incoming = captor.getAllValues().get(1);
        assertEquals(TransactionType.TRANSFER_OUT, outgoing.getType());
        assertEquals(FROM_ACCOUNT_ID, outgoing.getAccountId());
        assertMoney("400.00", outgoing.getBalanceAfter());
        assertEquals(TransactionType.TRANSFER_IN, incoming.getType());
        assertEquals(TO_ACCOUNT_ID, incoming.getAccountId());
        assertMoney("150.00", incoming.getBalanceAfter());
    }

    @Test
    void transfer_senderNotOwner_throwsResourceNotFound() {
        when(accountRepository.findById(TO_ACCOUNT_ID))
                .thenReturn(Optional.of(account(RECEIVER_USER_ID, "50.00")));
        when(accountBalanceRepository.withdraw(FROM_ACCOUNT_ID, "someone-else", new BigDecimal("10.00")))
                .thenReturn(Optional.empty());
        when(accountRepository.existsByIdAndUserId(FROM_ACCOUNT_ID, "someone-else")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> transferService.transfer(
                FROM_ACCOUNT_ID, "someone-else", TO_ACCOUNT_ID, new BigDecimal("10.00"), null));

        verify(accountBalanceRepository, never()).deposit(any(), any(), any());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transfer_receiverNotFound_throwsResourceNotFound_beforeDebitingSender() {
        when(accountRepository.findById(TO_ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transferService.transfer(
                FROM_ACCOUNT_ID, SENDER_USER_ID, TO_ACCOUNT_ID, new BigDecimal("10.00"), null));

        // Receiver is checked FIRST, so a missing receiver must never touch the sender's balance.
        verify(accountBalanceRepository, never()).withdraw(any(), any(), any());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transfer_selfTransfer_throwsInvalidRequest_beforeTouchingAnyAccount() {
        assertThrows(InvalidRequestException.class, () -> transferService.transfer(
                FROM_ACCOUNT_ID, SENDER_USER_ID, FROM_ACCOUNT_ID, new BigDecimal("10.00"), null));

        verify(accountRepository, never()).findById(any());
        verify(accountBalanceRepository, never()).withdraw(any(), any(), any());
        verify(accountBalanceRepository, never()).deposit(any(), any(), any());
    }

    @Test
    void transfer_insufficientFunds_throwsAndRecordsNothing() {
        when(accountRepository.findById(TO_ACCOUNT_ID))
                .thenReturn(Optional.of(account(RECEIVER_USER_ID, "50.00")));
        when(accountBalanceRepository.withdraw(FROM_ACCOUNT_ID, SENDER_USER_ID, new BigDecimal("999.00")))
                .thenReturn(Optional.empty());
        when(accountRepository.existsByIdAndUserId(FROM_ACCOUNT_ID, SENDER_USER_ID)).thenReturn(true);

        assertThrows(InsufficientFundsException.class, () -> transferService.transfer(
                FROM_ACCOUNT_ID, SENDER_USER_ID, TO_ACCOUNT_ID, new BigDecimal("999.00"), null));

        verify(accountBalanceRepository, never()).deposit(any(), any(), any());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transfer_receiverDepositFailsAfterSenderDebited_throwsAndRecordsNothing() {
        // A rare race: the receiver account existed when checked, but the locked update
        // still matches nothing (e.g. deleted a moment later). This must fail loudly rather
        // than silently losing the sender's money - @Transactional is what actually undoes
        // the sender's debit (verifiable against a real PostgreSQL database).
        when(accountRepository.findById(TO_ACCOUNT_ID))
                .thenReturn(Optional.of(account(RECEIVER_USER_ID, "50.00")));
        when(accountBalanceRepository.withdraw(FROM_ACCOUNT_ID, SENDER_USER_ID, new BigDecimal("100.00")))
                .thenReturn(Optional.of(account(SENDER_USER_ID, "400.00")));
        when(accountBalanceRepository.deposit(TO_ACCOUNT_ID, RECEIVER_USER_ID, new BigDecimal("100.00")))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transferService.transfer(
                FROM_ACCOUNT_ID, SENDER_USER_ID, TO_ACCOUNT_ID, new BigDecimal("100.00"), null));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transfer_nonPositiveAmount_isRejectedBeforeTouchingTheDatabase() {
        assertThrows(IllegalArgumentException.class, () -> transferService.transfer(
                FROM_ACCOUNT_ID, SENDER_USER_ID, TO_ACCOUNT_ID, BigDecimal.ZERO, null));

        verify(accountRepository, never()).findById(any());
    }
}
