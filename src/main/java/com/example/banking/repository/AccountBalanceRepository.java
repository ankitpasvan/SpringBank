package com.example.banking.repository;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.example.banking.model.Account;

/**
 * Changes an account balance with ONE atomic MongoDB operation ($inc).
 *
 * Why not "read account, change balance in Java, save"? Two requests at the same moment
 * would both read the old balance and one update would be lost (race condition).
 * With $inc the database itself does the arithmetic, so no update can be lost.
 */
@Repository
public class AccountBalanceRepository {

    private final MongoTemplate mongoTemplate;

    public AccountBalanceRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /** Adds the amount. Returns the updated account, or empty if the account does not exist. */
    public Optional<Account> deposit(String accountId, BigDecimal amount) {
        Query query = Query.query(Criteria.where("id").is(accountId));
        return applyBalanceChange(query, amount);
    }

    /**
     * Subtracts the amount ONLY IF balance >= amount (the check and the update are one atomic step,
     * so the balance can never go negative). Returns empty if the account does not exist
     * OR the balance is too low.
     */
    public Optional<Account> withdraw(String accountId, BigDecimal amount) {
        Query query = Query.query(Criteria.where("id").is(accountId).and("balance").gte(amount));
        return applyBalanceChange(query, amount.negate());
    }

    private Optional<Account> applyBalanceChange(Query query, BigDecimal delta) {
        Update update = new Update()
                .inc("balance", delta)
                .set("updatedAt", Instant.now());
        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true);
        return Optional.ofNullable(mongoTemplate.findAndModify(query, update, options, Account.class));
    }
}
