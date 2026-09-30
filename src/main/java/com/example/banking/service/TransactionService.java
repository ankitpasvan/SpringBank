package com.example.banking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.banking.dto.TransactionResponse;
import com.example.banking.exception.InsufficientFundsException;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.model.Account;
import com.example.banking.model.Transaction;
import com.example.banking.model.TransactionType;
import com.example.banking.repository.AccountBalanceRepository;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.TransactionRepository;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private static final int MONEY_SCALE = 2;

    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(AccountBalanceRepository accountBalanceRepository,
                              AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountBalanceRepository = accountBalanceRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public TransactionResponse deposit(String accountId, String userId, BigDecimal amount, String description) {
        BigDecimal money = normalize(amount);

        // Empty result covers BOTH "no such account" and "account exists but isn't yours" -
        // both are reported as the same "not found" (see AccountBalanceRepository).
        Account updated = accountBalanceRepository.deposit(accountId, userId, money)
                .orElseThrow(() -> accountNotFound(accountId));

        return recordTransaction(accountId, TransactionType.DEPOSIT, money, updated, description);
    }

    public TransactionResponse withdraw(String accountId, String userId, BigDecimal amount, String description) {
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

    private ResourceNotFoundException accountNotFound(String accountId) {
        return new ResourceNotFoundException("Account not found with id: " + accountId);
    }
}