package com.example.banking.repository;



import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.banking.model.Transaction;

public interface TransactionRepository extends MongoRepository<Transaction, String> {
}
