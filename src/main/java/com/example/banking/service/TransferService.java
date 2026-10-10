package com.example.banking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.banking.dto.TransferResponse;
import com.example.banking.exception.InsufficientFundsException;
import com.example.banking.exception.InvalidRequestException;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.model.Account;
import com.example.banking.model.Transaction;
import com.example.banking.model.TransactionType;
import com.example.banking.repository.AccountBalanceRepository;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.TransactionRepository;

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

    public TransferService(AccountBalanceRepository accountBalanceRepository, AccountRepository accountRepository,
                           TransactionRepository transactionRepository) {
        this.accountBalanceRepository = accountBalanceRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransferResponse transfer(String fromAccountId, String userId, String toAccountId, BigDecimal amount,
                                     String description) {
        BigDecimal money = normalize(amount);

        if (fromAccountId.equals(toAccountId)) {
            throw new InvalidRequestException("Cannot transfer to the same account");
        }

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

    // Defensive check: the controller will validate too, but money code must not trust its callers.
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
