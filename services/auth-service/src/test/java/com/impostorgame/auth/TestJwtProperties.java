package com.impostorgame.auth;

import com.impostorgame.auth.infrastructure.config.JwtProperties;
import org.springframework.core.io.ByteArrayResource;

import java.time.Duration;

public final class TestJwtProperties {

    public static final Duration EXPIRATION = Duration.ofHours(24);
    public static final Duration GUEST_EXPIRATION = Duration.ofHours(4);
    public static final Duration REFRESH_EXPIRATION = Duration.ofDays(30);

    private TestJwtProperties() {
    }

    public static JwtProperties create() {
        return new JwtProperties(
                new ByteArrayResource("private".getBytes()),
                new ByteArrayResource("public".getBytes()),
                "auth-key-1",
                EXPIRATION,
                GUEST_EXPIRATION,
                REFRESH_EXPIRATION
        );
    }
}