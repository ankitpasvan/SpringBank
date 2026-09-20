package com.example.banking.model;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB document stored in the "users" collection.
 * Only the password HASH is stored, never the plaintext password.
 */
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String name;

    // unique index = database-level safety net against duplicate emails
    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private Instant createdAt;

    private Instant updatedAt;

    public User(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
