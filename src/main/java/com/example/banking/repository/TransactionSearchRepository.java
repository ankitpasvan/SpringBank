package com.example.banking.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.example.banking.model.Transaction;
import com.example.banking.model.TransactionType;

/**
 * Searches the transactions of ONE account with optional filters and pagination.
 * The filters are optional, so the query is built step by step (a derived method name
 * like findByAccountIdAndTypeAndCreatedAtBetween would need one method per filter combination).
 */
@Repository
public class TransactionSearchRepository {

    private final MongoTemplate mongoTemplate;

    public TransactionSearchRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * @param type        optional: only this transaction type
     * @param from        optional: createdAt >= from (inclusive)
     * @param toExclusive optional: createdAt < toExclusive
     */
    public Page<Transaction> search(String accountId, TransactionType type, Instant from,
                                    Instant toExclusive, Pageable pageable) {
        Query filter = new Query(Criteria.where("accountId").is(accountId));

        if (type != null) {
            filter.addCriteria(Criteria.where("type").is(type));
        }
        if (from != null || toExclusive != null) {
            Criteria createdAt = Criteria.where("createdAt");
            if (from != null) {
                createdAt.gte(from);
            }
            if (toExclusive != null) {
                createdAt.lt(toExclusive);
            }
            filter.addCriteria(createdAt);
        }

        // 1) total number of matching rows (needed for totalPages), without skip/limit
        long total = mongoTemplate.count(filter, Transaction.class);

        // 2) only the requested page: same filter + skip/limit/sort from the Pageable
        List<Transaction> content = mongoTemplate.find(Query.of(filter).with(pageable), Transaction.class);

        return new PageImpl<>(content, pageable, total);
    }
}
