package com.impostorgame.auth.domain.model;

import com.impostorgame.auth.domain.exception.InvalidUserException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class User {
    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final String displayName;
    private final Role role;
    private final Instant createdAt;

    private User(UUID id, String email, String passwordHash, String displayName, Role role, Instant createdAt) {

        if (id == null) throw new InvalidUserException("id must not be null");
        if (email == null || email.isBlank()) throw new InvalidUserException("Email must not be null or blank");
        if (passwordHash == null || passwordHash.isBlank())
            throw new InvalidUserException("Password hash must not be null or blank");
        if (displayName == null || displayName.isBlank())
            throw new InvalidUserException("Display name must not be null or blank");
        if (displayName.length() < 3 || displayName.length() > 50)
            throw new InvalidUserException("Display name length must be between 3 and 50 characters");
        if (role == null) throw new InvalidUserException("Role must not be null");
        if (createdAt == null) throw new InvalidUserException("createdAt must not be null");

        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.createdAt = createdAt;
    }


    public static User create(String email, String passwordHash, String displayName, Role role) {
        return new User(UUID.randomUUID(), email, passwordHash, displayName, role, Instant.now());
    }

    public static User restore(UUID id, String email, String passwordHash, String displayName, Role role, Instant createdAt) {
        return new User(id, email, passwordHash, displayName, role, createdAt);
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String displayName() {
        return displayName;
    }

    public Role role() {
        return role;
    }

    public Instant createdAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof User user) && Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
}
