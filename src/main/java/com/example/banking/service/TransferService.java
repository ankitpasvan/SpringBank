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

        Account receiverAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> accountNotFound(toAccountId));


        Optional<Account> debitedSender = accountBalanceRepository.withdraw(fromAccountId, userId, money);
        if (debitedSender.isEmpty()) {
            if (!accountRepository.existsByIdAndUserId(fromAccountId, userId)) {
                throw accountNotFound(fromAccountId);
            }
            log.info("Transfer rejected from account id={}: insufficient funds", fromAccountId);
            throw new InsufficientFundsException();
        }


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


    private TransferResponse replayIfFingerprintMatches(IdempotencyRecord claim, String fingerprint) {
        String stored = claim.getRequestFingerprint();
        if (!fingerprint.equals(stored)) {
            log.info("Idempotency key conflict for claim id={}: same key, different request payload",
                    claim.getId());
            throw new IdempotencyKeyConflictException();
        }
        return replay(claim);
    }


    private TransferResponse replay(IdempotencyRecord claim) {
        Transaction outgoing = transactionRepository.findById(claim.getResultTransactionId())
                .orElseThrow(() -> new IllegalStateException(
                        "Idempotency record references a missing transaction: " + claim.getResultTransactionId()));
        return new TransferResponse(outgoing.getId(), outgoing.getAccountId(), claim.getToAccountId(),
                outgoing.getAmount(), outgoing.getBalanceAfter(), outgoing.getDescription(),
                outgoing.getCreatedAt());
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
