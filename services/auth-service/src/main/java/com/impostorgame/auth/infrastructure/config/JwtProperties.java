package com.impostorgame.auth.infrastructure.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.Duration;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(

        Resource privateKeyLocation,

        Resource publicKeyLocation,

        String kid,

        Duration expiration,

        Duration guestExpiration,

        Duration refreshExpiration
) {

    public JwtProperties {
        if (privateKeyLocation == null) throw new IllegalArgumentException("jwt.privateKeyLocation must not be null");
        if (publicKeyLocation == null) throw new IllegalArgumentException("jwt.publicKeyLocation must not be null");
        if (kid == null || kid.isBlank()) throw new IllegalArgumentException("jwt.kid must not be null or blank");
        if (expiration == null || expiration.isNegative() || expiration.isZero())
            throw new IllegalArgumentException("jwt.expiration must be positive");
        if (guestExpiration == null || guestExpiration.isNegative() || guestExpiration.isZero())
            throw new IllegalArgumentException("jwt.guestExpiration must be positive");

        if (refreshExpiration == null || refreshExpiration.isNegative() || refreshExpiration.isZero())
            throw new IllegalArgumentException("jwt.refreshExpiration must be positive");

    }
}
