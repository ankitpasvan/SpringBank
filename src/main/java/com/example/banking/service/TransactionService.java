package com.example.banking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


    @Transactional
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
    @Transactional
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


        boolean balanceMutated = false;
        try {
            Optional<Account> updated = accountBalanceRepository.withdraw(accountId, userId, money);
            if (updated.isEmpty()) {

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


        Account updated = accountBalanceRepository.deposit(accountId, userId, money)
                .orElseThrow(() -> accountNotFound(accountId));

        return recordTransaction(accountId, TransactionType.DEPOSIT, money, updated, description);
    }

    private TransactionResponse executeWithdraw(String accountId, String userId, BigDecimal amount,
                                                String description) {
        BigDecimal money = normalize(amount);

        Optional<Account> updated = accountBalanceRepository.withdraw(accountId, userId, money);
        if (updated.isEmpty()) {

            if (!accountRepository.existsByIdAndUserId(accountId, userId)) {
                throw accountNotFound(accountId);
            }
            log.info("Withdrawal rejected for account id={}: insufficient funds", accountId);
            throw new InsufficientFundsException();
        }

        return recordTransaction(accountId, TransactionType.WITHDRAWAL, money, updated.get(), description);
    }


    private TransactionResponse replayIfFingerprintMatches(IdempotencyRecord claim, String fingerprint) {
        String stored = claim.getRequestFingerprint();
        if (stored == null) {

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

        String fingerprint = IdempotencyFingerprint.of(transaction.getAccountId(), transaction.getType(),
                transaction.getAmount(), transaction.getDescription());
        return idempotencyService.ensureFingerprint(claim.getId(), fingerprint);
    }


    private TransactionResponse replay(IdempotencyRecord claim) {
        Transaction transaction = transactionRepository.findById(claim.getResultTransactionId())
                .orElseThrow(() -> new IllegalStateException(
                        "Idempotency record references a missing transaction: " + claim.getResultTransactionId()));
        return TransactionResponse.from(transaction);
    }

    private TransactionResponse recordTransaction(String accountId, TransactionType type, BigDecimal amount,
                                                  Account updatedAccount, String description) {

        Transaction saved = transactionRepository.save(
                new Transaction(accountId, type, amount, updatedAccount.getBalance(), description));
        log.info("Transaction {} recorded: type={}, accountId={}, amount={}",
                saved.getId(), type, accountId, amount);
        return TransactionResponse.from(saved);
    }


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
