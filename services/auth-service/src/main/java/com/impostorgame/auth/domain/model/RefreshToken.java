package com.impostorgame.auth.domain.model;

import com.impostorgame.auth.domain.exception.InvalidRefreshTokenException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class RefreshToken {
    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private final Instant createdAt;

    private RefreshToken(UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant createdAt) {
        if (id == null) throw new InvalidRefreshTokenException("id must not be null");
        if (userId == null) throw new InvalidRefreshTokenException("userId must not be null");
        if (tokenHash == null || tokenHash.isBlank()) throw new InvalidRefreshTokenException("token must not be null or blank");
        if (expiresAt == null) throw new InvalidRefreshTokenException("expiresAt must not be null");
        if (createdAt == null) throw new InvalidRefreshTokenException("createdAt must not be null");
        if (!expiresAt.isAfter(createdAt)) throw new InvalidRefreshTokenException("expiresAt must be after createdAt");

        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public static RefreshToken create(UUID userId, String token, Duration ttl) {
        Instant now = Instant.now();
        return new RefreshToken(UUID.randomUUID(), userId, token, now.plus(ttl), now);
    }

    public static RefreshToken restore(UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant createdAt) {
        return new RefreshToken(id, userId, tokenHash, expiresAt, createdAt);
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof RefreshToken rt) && id.equals(rt.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
