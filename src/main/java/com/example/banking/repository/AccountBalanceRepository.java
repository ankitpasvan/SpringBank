package com.example.banking.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.banking.model.Account;

/**
 * Changes an account balance atomically using a pessimistic row lock (SELECT ... FOR UPDATE).
 *
 * Why not "read account, change balance in Java, save"? Two requests at the same moment
 * would both read the old balance and one update would be lost (race condition).
 * With the row locked, the database serializes concurrent writers: the second one waits
 * for the first one's transaction to finish, then sees the updated balance. No update
 * can be lost, and the balance can never go negative.
 *
 * The ownership check (userId) happens AFTER the lock is acquired but BEFORE any change,
 * so a caller can never observe or race against "is this mine" as a distinct step.
 */
@Repository
public class AccountBalanceRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Adds the amount. Returns the updated account, or empty if no account with this id
     * AND this owner exists.
     */
    @Transactional
    public Optional<Account> deposit(String accountId, String userId, BigDecimal amount) {
        Account account = findOwnedForUpdate(accountId, userId);
        if (account == null) {
            return Optional.empty();
        }
        account.setBalance(account.getBalance().add(amount));
        account.setUpdatedAt(Instant.now());
        return Optional.of(account);
    }

    /**
     * Subtracts the amount ONLY IF the account belongs to userId AND balance >= amount (the
     * ownership check, the balance check and the update all happen while the row is locked,
     * so the balance can never go negative and ownership can never be bypassed by a race).
     * Returns empty if the account does not exist, is not owned by userId, or has too
     * little balance.
     */
    @Transactional
    public Optional<Account> withdraw(String accountId, String userId, BigDecimal amount) {
        Account account = findOwnedForUpdate(accountId, userId);
        if (account == null || account.getBalance().compareTo(amount) < 0) {
            return Optional.empty();
        }
        account.setBalance(account.getBalance().subtract(amount));
        account.setUpdatedAt(Instant.now());
        return Optional.of(account);
    }

    /**
     * Loads the account with a write lock, or returns null when there is no such account
     * or it is not owned by userId. Callers treat null as "not found" without distinguishing
     * the two cases (see AccountService for why).
     */
    private Account findOwnedForUpdate(String accountId, String userId) {
        Account account = entityManager.find(Account.class, accountId, LockModeType.PESSIMISTIC_WRITE);
        if (account == null || !account.getUserId().equals(userId)) {
            return null;
        }
        return account;
    }
}
