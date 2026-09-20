package com.example.banking.repository;


import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.banking.model.User;

public interface UserRepository extends MongoRepository<User, String> {

    // Spring Data builds the query from the method name: { email: ? }
    boolean existsByEmail(String email);
}
