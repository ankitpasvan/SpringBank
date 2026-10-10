package com.example.banking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.banking.dto.TransactionResponse;
import com.example.banking.exception.IdempotencyKeyConflictException;
import com.example.banking.exception.InsufficientFundsException;
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

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private static final int MONEY_SCALE = 2;

    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final IdempotencyService idempotencyService;

    public TransactionService(AccountBalanceRepository accountBalanceRepository,
                              AccountRepository accountRepository, TransactionRepository transactionRepository,
                              IdempotencyService idempotencyService) {
        this.accountBalanceRepository = accountBalanceRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.idempotencyService = idempotencyService;
    }

    /**
     * @param idempotencyKey optional. Null/blank -> behaves exactly as before this feature
     *                       existed. Non-blank -> a retry with the SAME (userId, key) and the
     *                       SAME normalized payload replays the first attempt's result instead
     *                       of moving money again; the same key with a DIFFERENT amount or
     *                       description is rejected with 409 Conflict.
     */
    public TransactionResponse deposit(String accountId, String userId, BigDecimal amount, String description,
                                       String idempotencyKey) {
        BigDecimal money = normalize(amount);
        if (isBlank(idempotencyKey)) {
            return executeDeposit(accountId, userId, money, description);
        }

        String fingerprint = IdempotencyFingerprint.of(accountId, TransactionType.DEPOSIT, money, description);
        IdempotencyRecord claim = idempotencyService.claim(userId, idempotencyKey, fingerprint);
        if (claim.getStatus() == IdempotencyStatus.COMPLETED) {
            return replayIfFingerprintMatches(claim, fingerprint);
        }

        // The balance write and the ledger write below are separate. If the ledger write (or
        // the completion bookkeeping) fails AFTER the balance already moved, the claim must
        // NOT be released: releasing would let a retry move the money a second time. The
        // claim stays IN_PROGRESS (bounded by the 24h TTL) and fails closed instead.
        boolean balanceMutated = false;
        try {
            Account updated = accountBalanceRepository.deposit(accountId, userId, money)
                    .orElseThrow(() -> accountNotFound(accountId));
            balanceMutated = true;
            TransactionResponse response =
                    recordTransaction(accountId, TransactionType.DEPOSIT, money, updated, description);
            idempotencyService.markCompleted(claim.getId(), response.id(), null);
            return response;
        } catch (RuntimeException ex) {
            if (!balanceMutated) {
                idempotencyService.release(claim.getId());
            } else {
                log.warn("Deposit partially completed for idempotency claim {}: balance mutated but a "
                        + "later step failed; claim left IN_PROGRESS", claim.getId());
            }
            throw ex;
        }
    }

    /** @param idempotencyKey see {@link #deposit}. */
    public TransactionResponse withdraw(String accountId, String userId, BigDecimal amount, String description,
                                        String idempotencyKey) {
        BigDecimal money = normalize(amount);
        if (isBlank(idempotencyKey)) {
            return executeWithdraw(accountId, userId, money, description);
        }

        String fingerprint = IdempotencyFingerprint.of(accountId, TransactionType.WITHDRAWAL, money, description);
        IdempotencyRecord claim = idempotencyService.claim(userId, idempotencyKey, fingerprint);
        if (claim.getStatus() == IdempotencyStatus.COMPLETED) {
            return replayIfFingerprintMatches(claim, fingerprint);
        }

        // Same phase discipline as deposit(): only release the claim when the balance was
        // never touched. A withdrawal that matched nothing mutates nothing, so the
        // "no such account" vs "insufficient funds" distinction is unchanged.
        boolean balanceMutated = false;
        try {
            Optional<Account> updated = accountBalanceRepository.withdraw(accountId, userId, money);
            if (updated.isEmpty()) {
                // The atomic update matched nothing: either no such account, the account isn't
                // yours, or balance < amount. Only the last case is "insufficient funds".
                if (!accountRepository.existsByIdAndUserId(accountId, userId)) {
                    throw accountNotFound(accountId);
                }
                log.info("Withdrawal rejected for account id={}: insufficient funds", accountId);
                throw new InsufficientFundsException();
            }
            balanceMutated = true;
            TransactionResponse response =
                    recordTransaction(accountId, TransactionType.WITHDRAWAL, money, updated.get(), description);
            idempotencyService.markCompleted(claim.getId(), response.id(), null);
            return response;
        } catch (RuntimeException ex) {
            if (!balanceMutated) {
                idempotencyService.release(claim.getId());
            } else {
                log.warn("Withdrawal partially completed for idempotency claim {}: balance mutated but a "
                        + "later step failed; claim left IN_PROGRESS", claim.getId());
            }
            throw ex;
        }
    }

    private TransactionResponse executeDeposit(String accountId, String userId, BigDecimal amount,
                                               String description) {
        BigDecimal money = normalize(amount);

        // Empty result covers BOTH "no such account" and "account exists but isn't yours" -
        // both are reported as the same "not found" (see AccountBalanceRepository).
        Account updated = accountBalanceRepository.deposit(accountId, userId, money)
                .orElseThrow(() -> accountNotFound(accountId));

        return recordTransaction(accountId, TransactionType.DEPOSIT, money, updated, description);
    }

    private TransactionResponse executeWithdraw(String accountId, String userId, BigDecimal amount,
                                                String description) {
        BigDecimal money = normalize(amount);

        Optional<Account> updated = accountBalanceRepository.withdraw(accountId, userId, money);
        if (updated.isEmpty()) {
            // The atomic update matched nothing: either no such account, the account isn't
            // yours, or balance < amount. Only the last case is "insufficient funds".
            if (!accountRepository.existsByIdAndUserId(accountId, userId)) {
                throw accountNotFound(accountId);
            }
            log.info("Withdrawal rejected for account id={}: insufficient funds", accountId);
            throw new InsufficientFundsException();
        }

        return recordTransaction(accountId, TransactionType.WITHDRAWAL, money, updated.get(), description);
    }

    // A COMPLETED record means this exact key finished before. Replay the original result only
    // when the stored fingerprint matches this request's fingerprint; a mismatch means the
    // client reused the key for a genuinely different request -> 409, never a silent replay.
    private TransactionResponse replayIfFingerprintMatches(IdempotencyRecord claim, String fingerprint) {
        String stored = claim.getRequestFingerprint();
        if (stored == null) {
            // Legacy record written before fingerprinting existed: reconstruct the fingerprint
            // from the immutable transaction row it points to, persist it, then enforce.
            stored = backfillFingerprint(claim);
        }
        if (!stored.equals(fingerprint)) {
            log.info("Idempotency key conflict for claim id={}: same key, different request payload",
                    claim.getId());
            throw new IdempotencyKeyConflictException();
        }
        return replay(claim);
    }

    private String backfillFingerprint(IdempotencyRecord claim) {
        Transaction transaction = transactionRepository.findById(claim.getResultTransactionId())
                .orElseThrow(() -> new IllegalStateException(
                        "Idempotency record references a missing transaction: " + claim.getResultTransactionId()));
        // The transaction row stores exactly the normalized fields the original request was
        // fingerprinted with, so this reconstructs the identical fingerprint.
        String fingerprint = IdempotencyFingerprint.of(transaction.getAccountId(), transaction.getType(),
                transaction.getAmount(), transaction.getDescription());
        return idempotencyService.ensureFingerprint(claim.getId(), fingerprint);
    }

    // Reconstructs the ORIGINAL response from the Transaction row that the first (successful)
    // attempt already saved - no balance change, no new Transaction, just a read.
    private TransactionResponse replay(IdempotencyRecord claim) {
        Transaction transaction = transactionRepository.findById(claim.getResultTransactionId())
                .orElseThrow(() -> new IllegalStateException(
                        "Idempotency record references a missing transaction: " + claim.getResultTransactionId()));
        return TransactionResponse.from(transaction);
    }

    private TransactionResponse recordTransaction(String accountId, TransactionType type, BigDecimal amount,
                                                  Account updatedAccount, String description) {
        // Note: the balance change above and this insert are two separate writes.
        // Multi-document MongoDB transactions arrive with the transfer feature.
        Transaction saved = transactionRepository.save(
                new Transaction(accountId, type, amount, updatedAccount.getBalance(), description));
        log.info("Transaction {} recorded: type={}, accountId={}, amount={}",
                saved.getId(), type, accountId, amount);
        return TransactionResponse.from(saved);
    }

    // Defensive check: the controller already validates, but money code must not trust its callers.
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
