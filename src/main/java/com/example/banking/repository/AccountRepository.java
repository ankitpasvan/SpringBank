package com.example.banking.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.banking.model.Account;

public interface AccountRepository extends MongoRepository<Account, String> {

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findByUserId(String userId);
}
