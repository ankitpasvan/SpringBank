package com.example.banking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.model.User;

public interface UserRepository extends JpaRepository<User, String> {

    // Spring Data builds the query from the method name: where email = ?
    boolean existsByEmail(String email);

    // Needed for login: look the user up by email so we can check their password hash.
    Optional<User> findByEmail(String email);
}
