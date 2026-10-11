package com.example.banking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

import com.example.banking.dto.TransferResponse;
import com.example.banking.exception.IdempotencyInProgressException;
import com.example.banking.exception.IdempotencyKeyConflictException;
import com.example.banking.exception.InsufficientFundsException;
import com.example.banking.model.Account;
import com.example.banking.model.AccountType;
import com.example.banking.model.IdempotencyRecord;
import com.example.banking.model.Transaction;
import com.example.banking.model.TransactionType;
import com.example.banking.repository.AccountBalanceRepository;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.TransactionRepository;
import com.example.banking.util.IdempotencyFingerprint;

// Service-level idempotency tests for transfers, mirroring the deposit/withdraw
// idempotency contract: same key + same params -> replay, same key + different
// params -> 409, concurrent same key -> 409 "in progress".
//
// NOTE on concurrency: true multi-threaded safety lives inside
// IdempotencyService.claim (unique-constraint race), which IdempotencyConcurrencyTest
// already covers. What is tested here is the transfer-level wiring: while another
// request holds the key, this request must not move money.
@ExtendWith(MockitoExtension.class)
class TransferIdempotencyTest {

    private static final String FROM = "acc-sender";
    private static final String TO = "acc-receiver";
    private static final String SENDER = "user-1";
    private static final String RECEIVER = "user-2";
    private static final String KEY = "key-123";
    private static final BigDecimal AMOUNT = new BigDecimal("100.00");
    private static final String DESCRIPTION = "rent split";

    @Mock
    private AccountBalanceRepository accountBalanceRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private IdempotencyService idempotencyService;

    private TransferService transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferService(accountBalanceRepository, accountRepository, transactionRepository,
                idempotencyService);
    }

    private Account account(String userId, String balance) {
        return new Account("123456789012", userId, AccountType.SAVINGS, new BigDecimal(balance));
    }

    private String fingerprint() {
        return IdempotencyFingerprint.ofTransfer(FROM, TO, AMOUNT, DESCRIPTION);
    }

    private IdempotencyRecord inProgressClaim() {
        IdempotencyRecord record = new IdempotencyRecord(SENDER, KEY, fingerprint());
        ReflectionTestUtils.setField(record, "id", "claim-1");
        return record;
    }

    private IdempotencyRecord completedClaim(String storedFingerprint) {
        IdempotencyRecord record = new IdempotencyRecord(SENDER, KEY, storedFingerprint);
        ReflectionTestUtils.setField(record, "id", "claim-1");
        record.markCompleted("tx-1", TO);
        return record;
    }

    private Transaction storedOutgoingTransfer() {
        Transaction tx = new Transaction(FROM, TransactionType.TRANSFER_OUT, AMOUNT, new BigDecimal("400.00"),
                DESCRIPTION);
        ReflectionTestUtils.setField(tx, "id", "tx-1");
        return tx;
    }

    /** Stubs a fully successful transfer execution (debit + credit + ledger). */
    private void stubSuccessfulExecution() {
        when(accountRepository.findById(TO)).thenReturn(Optional.of(account(RECEIVER, "50.00")));
        when(accountBalanceRepository.withdraw(FROM, SENDER, AMOUNT))
                .thenReturn(Optional.of(account(SENDER, "400.00")));
        when(accountBalanceRepository.deposit(TO, RECEIVER, AMOUNT))
                .thenReturn(Optional.of(account(RECEIVER, "150.00")));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction tx = inv.getArgument(0);
            if (tx.getType() == TransactionType.TRANSFER_OUT) {
                ReflectionTestUtils.setField(tx, "id", "tx-1");
            }
            return tx;
        });
    }

    @Test
    void transfer_firstTimeWithNewKey_claimsExecutesAndCompletes() {
        when(idempotencyService.claim(eq(SENDER), eq(KEY), anyString())).thenReturn(inProgressClaim());
        stubSuccessfulExecution();

        TransferResponse response = transferService.transfer(FROM, SENDER, TO, AMOUNT, DESCRIPTION, KEY);

        assertEquals("tx-1", response.transactionId());
        assertEquals(FROM, response.fromAccountId());
        assertEquals(TO, response.toAccountId());
        assertEquals(0, AMOUNT.compareTo(response.amount()));
        // The claim is completed with the outgoing transaction id AND the receiver, so a
        // later replay can rebuild the full TransferResponse.
        verify(idempotencyService).markCompleted("claim-1", "tx-1", TO);
    }

    @Test
    void transfer_sameKeySameParams_replaysWithoutMovingMoneyAgain() {
        when(idempotencyService.claim(eq(SENDER), eq(KEY), anyString()))
                .thenReturn(completedClaim(fingerprint()));
        when(transactionRepository.findById("tx-1")).thenReturn(Optional.of(storedOutgoingTransfer()));

        TransferResponse response = transferService.transfer(FROM, SENDER, TO, AMOUNT, DESCRIPTION, KEY);

        assertEquals("tx-1", response.transactionId());
        assertEquals(FROM, response.fromAccountId());
        assertEquals(TO, response.toAccountId());
        assertEquals(0, AMOUNT.compareTo(response.amount()));
        assertEquals(0, new BigDecimal("400.00").compareTo(response.senderBalanceAfter()));
        // Replay is read-only: no balance change, no new ledger rows, no re-completion.
        verify(accountBalanceRepository, never()).withdraw(any(), any(), any());
        verify(accountBalanceRepository, never()).deposit(any(), any(), any());
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(idempotencyService, never()).markCompleted(any(), any(), any());
    }

    @Test
    void transfer_sameKeyDifferentParams_rejectedWith409() {
        String differentFingerprint = IdempotencyFingerprint.ofTransfer(FROM, "acc-other", AMOUNT, DESCRIPTION);
        when(idempotencyService.claim(eq(SENDER), eq(KEY), anyString()))
                .thenReturn(completedClaim(differentFingerprint));

        assertThrows(IdempotencyKeyConflictException.class,
                () -> transferService.transfer(FROM, SENDER, TO, AMOUNT, DESCRIPTION, KEY));

        verify(accountBalanceRepository, never()).withdraw(any(), any(), any());
        verify(accountBalanceRepository, never()).deposit(any(), any(), any());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transfer_keyInProgressByConcurrentRequest_doesNotExecute() {
        when(idempotencyService.claim(eq(SENDER), eq(KEY), anyString()))
                .thenThrow(new IdempotencyInProgressException());

        assertThrows(IdempotencyInProgressException.class,
                () -> transferService.transfer(FROM, SENDER, TO, AMOUNT, DESCRIPTION, KEY));

        verify(accountBalanceRepository, never()).withdraw(any(), any(), any());
        verify(accountBalanceRepository, never()).deposit(any(), any(), any());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transfer_failedTransfer_releasesClaimAndWritesNothing() {
        when(idempotencyService.claim(eq(SENDER), eq(KEY), anyString())).thenReturn(inProgressClaim());
        when(accountRepository.findById(TO)).thenReturn(Optional.of(account(RECEIVER, "50.00")));
        when(accountBalanceRepository.withdraw(FROM, SENDER, AMOUNT)).thenReturn(Optional.empty());
        when(accountRepository.existsByIdAndUserId(FROM, SENDER)).thenReturn(true);

        assertThrows(InsufficientFundsException.class,
                () -> transferService.transfer(FROM, SENDER, TO, AMOUNT, DESCRIPTION, KEY));

        // Balance was never touched, so the key is released for a clean retry...
        verify(idempotencyService).release("claim-1");
        // ...and nothing partial was persisted.
        verify(idempotencyService, never()).markCompleted(any(), any(), any());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transfer_claimIsScopedToCallingUser() {
        when(idempotencyService.claim(eq(SENDER), eq(KEY), anyString())).thenReturn(inProgressClaim());
        stubSuccessfulExecution();

        transferService.transfer(FROM, SENDER, TO, AMOUNT, DESCRIPTION, KEY);

        // The claim is filed under the CALLER's user id; the (userId, key) unique
        // constraint then guarantees one user can never see or replay another user's
        // transfer result, even with an identical key string.
        ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(idempotencyService).claim(userIdCaptor.capture(), eq(KEY), anyString());
        assertEquals(SENDER, userIdCaptor.getValue());
    }

    @Test
    void transfer_nullKey_skipsIdempotencyEntirely() {
        stubSuccessfulExecution();

        TransferResponse response = transferService.transfer(FROM, SENDER, TO, AMOUNT, DESCRIPTION, null);

        assertEquals("tx-1", response.transactionId());
        verifyNoInteractions(idempotencyService);
    }

    @Test
    void transfer_blankKey_skipsIdempotencyEntirely() {
        stubSuccessfulExecution();

        transferService.transfer(FROM, SENDER, TO, AMOUNT, DESCRIPTION, "   ");

        verifyNoInteractions(idempotencyService);
    }
}
