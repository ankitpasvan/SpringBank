package com.example.banking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.banking.dto.TransferResponse;
import com.example.banking.exception.IdempotencyKeyConflictException;
import com.example.banking.exception.InsufficientFundsException;
import com.example.banking.exception.InvalidRequestException;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.model.Account;
import com.example.banking.model.IdempotencyRecord;
import com.example.banking.model.IdempotencyStatus;
import com.example.banking.model.Transaction;
import com.example.banking.model.TransactionType;
import com.example.banking.repository.AccountBalanceRepository;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.TransactionRepository;
import com.example.banking.util.IdempotencyFingerprint;

/**
 * Moves money from one account to another as a single all-or-nothing operation.
 *
 * WHY @Transactional: a transfer touches TWO accounts (debit one, credit the other) plus
 * TWO transaction records. Deposit/withdraw only ever touch one account, so a single locked
 * row update is enough there (see AccountBalanceRepository). Here, if the process crashed
 * right after debiting the sender but before crediting the receiver, money would simply
 * vanish - @Transactional (backed by Spring Boot's auto-configured JpaTransactionManager)
 * makes PostgreSQL itself undo the debit if anything later in this method throws.
 *
 * Unlike the previous MongoDB implementation, this needs no replica set: PostgreSQL
 * supports multi-statement transactions on any installation.
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private static final int MONEY_SCALE = 2;

    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final IdempotencyService idempotencyService;

    public TransferService(AccountBalanceRepository accountBalanceRepository,
                           AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           IdempotencyService idempotencyService) {
        this.accountBalanceRepository = accountBalanceRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.idempotencyService = idempotencyService;
    }

    /**
     * @param idempotencyKey optional. Null/blank -> behaves exactly as before: the transfer
     *                       runs with no idempotency tracking. Non-blank -> a retry with the
     *                       SAME key and the SAME transfer parameters replays the first
     *                       attempt's result instead of moving money again; the same key with
     *                       DIFFERENT parameters is rejected with 409 Conflict.
     *
     *                       The claim is scoped to (userId, idempotencyKey) by the database
     *                       unique constraint, so one user can never replay another user's
     *                       transfer result.
     */
    @Transactional
    public TransferResponse transfer(String fromAccountId, String userId, String toAccountId, BigDecimal amount,
                                     String description, String idempotencyKey) {
        BigDecimal money = normalize(amount);

        if (fromAccountId.equals(toAccountId)) {
            throw new InvalidRequestException("Cannot transfer to the same account");
        }

        if (isBlank(idempotencyKey)) {
            return executeTransfer(fromAccountId, userId, toAccountId, money, description);
        }

        String fingerprint = IdempotencyFingerprint.ofTransfer(fromAccountId, toAccountId, money, description);
        IdempotencyRecord claim = idempotencyService.claim(userId, idempotencyKey, fingerprint);
        if (claim.getStatus() == IdempotencyStatus.COMPLETED) {
            return replayIfFingerprintMatches(claim, fingerprint);
        }

        // Same phase discipline as TransactionService.deposit(): only release the claim when
        // the balance was never touched. Everything below runs in ONE database transaction,
        // so any failure also rolls the debit/credit back - and the COMPLETED mark is part
        // of that same transaction, so it can never be persisted without the money movement.
        boolean balanceMutated = false;
        try {
            TransferResponse response = executeTransfer(fromAccountId, userId, toAccountId, money, description);
            balanceMutated = true;
            idempotencyService.markCompleted(claim.getId(), response.transactionId(), toAccountId);
            return response;
        } catch (RuntimeException ex) {
            if (!balanceMutated) {
                idempotencyService.release(claim.getId());
            } else {
                log.warn("Transfer partially completed for idempotency claim {}: balance mutated but a "
                        + "later step failed; claim left IN_PROGRESS", claim.getId());
            }
            throw ex;
        }
    }

    private TransferResponse executeTransfer(String fromAccountId, String userId, String toAccountId,
                                             BigDecimal money, String description) {
        // Checked BEFORE touching the sender's balance: a missing receiver must never
        // debit the sender in the first place.
        Account receiverAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> accountNotFound(toAccountId));

        // Ownership + balance guard, exactly like TransactionService.withdraw - the sender
        // must own fromAccountId and have enough balance, checked while the row is locked.
        Optional<Account> debitedSender = accountBalanceRepository.withdraw(fromAccountId, userId, money);
        if (debitedSender.isEmpty()) {
            if (!accountRepository.existsByIdAndUserId(fromAccountId, userId)) {
                throw accountNotFound(fromAccountId);
            }
            log.info("Transfer rejected from account id={}: insufficient funds", fromAccountId);
            throw new InsufficientFundsException();
        }

        // The receiver does NOT need to be owned by this caller - anyone can be sent money.
        // We use the receiver's OWN userId (just fetched above), never the sender's.
        Account creditedReceiver = accountBalanceRepository.deposit(toAccountId, receiverAccount.getUserId(), money)
                .orElseThrow(() -> accountNotFound(toAccountId));

        Transaction outgoing = transactionRepository.save(new Transaction(fromAccountId, TransactionType.TRANSFER_OUT,
                money, debitedSender.get().getBalance(), description));
        transactionRepository.save(new Transaction(toAccountId, TransactionType.TRANSFER_IN,
                money, creditedReceiver.getBalance(), description));

        log.info("Transfer {} recorded: from={}, to={}, amount={}", outgoing.getId(), fromAccountId, toAccountId,
                money);

        return new TransferResponse(outgoing.getId(), fromAccountId, toAccountId, money,
                debitedSender.get().getBalance(), description, outgoing.getCreatedAt());
    }

    // Replays the ORIGINAL response when the stored fingerprint matches this request's
    // fingerprint; a mismatch means the client reused the key for a genuinely different
    // request -> 409, never a silent replay.
    //
    // Transfer claims are always created WITH a fingerprint (this feature never existed
    // without one), so there is no legacy backfill path like deposits have: a missing
    // fingerprint fails closed instead of being reconstructed.
    private TransferResponse replayIfFingerprintMatches(IdempotencyRecord claim, String fingerprint) {
        String stored = claim.getRequestFingerprint();
        if (!fingerprint.equals(stored)) {
            log.info("Idempotency key conflict for claim id={}: same key, different request payload",
                    claim.getId());
            throw new IdempotencyKeyConflictException();
        }
        return replay(claim);
    }

    // Reconstructs the ORIGINAL response from the TRANSFER_OUT row that the first
    // (successful) attempt already saved - no balance change, no new rows, just reads.
    // toAccountId comes from the claim itself, because the Transaction row stores no
    // destination account (that is exactly what IdempotencyRecord.toAccountId is for).
    private TransferResponse replay(IdempotencyRecord claim) {
        Transaction outgoing = transactionRepository.findById(claim.getResultTransactionId())
                .orElseThrow(() -> new IllegalStateException(
                        "Idempotency record references a missing transaction: " + claim.getResultTransactionId()));
        return new TransferResponse(outgoing.getId(), outgoing.getAccountId(), claim.getToAccountId(),
                outgoing.getAmount(), outgoing.getBalanceAfter(), outgoing.getDescription(),
                outgoing.getCreatedAt());
    }

    // Defensive check: the controller will validate too, but money code must not trust its callers.
    private BigDecimal normalize(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        return amount.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ResourceNotFoundException accountNotFound(String accountId) {
        return new ResourceNotFoundException("Account not found with id: " + accountId);
    }
}
