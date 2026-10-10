package com.example.banking.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.example.banking.model.Transaction;
import com.example.banking.model.TransactionType;

/**
 * Searches the transactions of ONE account with optional filters and pagination.
 * The filters are optional, so the query is built step by step with the JPA Criteria API
 * (a derived method name like findByAccountIdAndTypeAndCreatedAtBetween would need one
 * method per filter combination).
 */
@Repository
public class TransactionSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * @param type        optional: only this transaction type
     * @param from        optional: createdAt >= from (inclusive)
     * @param toExclusive optional: createdAt < toExclusive
     */
    public Page<Transaction> search(String accountId, TransactionType type, Instant from,
                                    Instant toExclusive, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // 1) total number of matching rows (needed for totalPages)
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Transaction> countRoot = countQuery.from(Transaction.class);
        countQuery.select(cb.count(countRoot))
                .where(filterPredicates(cb, countRoot, accountId, type, from, toExclusive));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        // 2) only the requested page: same filter + offset/limit/sort from the Pageable
        CriteriaQuery<Transaction> query = cb.createQuery(Transaction.class);
        Root<Transaction> root = query.from(Transaction.class);
        query.where(filterPredicates(cb, root, accountId, type, from, toExclusive));
        query.orderBy(sortOrders(cb, root, pageable.getSort()));

        TypedQuery<Transaction> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        List<Transaction> content = typedQuery.getResultList();

        return new PageImpl<>(content, pageable, total);
    }

    private Predicate[] filterPredicates(CriteriaBuilder cb, Root<Transaction> root, String accountId,
                                         TransactionType type, Instant from, Instant toExclusive) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("accountId"), accountId));
        if (type != null) {
            predicates.add(cb.equal(root.get("type"), type));
        }
        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.<Instant>get("createdAt"), from));
        }
        if (toExclusive != null) {
            predicates.add(cb.lessThan(root.<Instant>get("createdAt"), toExclusive));
        }
        return predicates.toArray(new Predicate[0]);
    }

    private List<Order> sortOrders(CriteriaBuilder cb, Root<Transaction> root, Sort sort) {
        List<Order> orders = new ArrayList<>();
        for (Sort.Order sortOrder : sort) {
            Path<?> path = root.get(sortOrder.getProperty());
            orders.add(sortOrder.isAscending() ? cb.asc(path) : cb.desc(path));
        }
        return orders;
    }
}
