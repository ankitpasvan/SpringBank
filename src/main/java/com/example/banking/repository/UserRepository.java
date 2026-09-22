package com.example.banking.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.banking.model.User;

public interface UserRepository extends MongoRepository<User, String> {

    // Spring Data builds the query from the method name: { email: ? }
    boolean existsByEmail(String email);

    // Needed for login: look the user up by email so we can check their password hash.
    Optional<User> findByEmail(String email);
}