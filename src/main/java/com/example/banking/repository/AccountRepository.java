package com.example.banking.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.banking.model.Account;

public interface AccountRepository extends MongoRepository<Account, String> {

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findByUserId(String userId);

    // Used to tell "no such account" apart from "account exists but isn't yours" without
    // fetching the whole document - both cases are still reported identically as "not found".
    boolean existsByIdAndUserId(String id, String userId);
}