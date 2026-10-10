package com.example.banking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.model.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
}
