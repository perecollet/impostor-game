package com.impostorgame.auth.infrastructure.config;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(

        Resource privateKeyLocation,

        @NotBlank
        String kid,

        @Positive
        long expiration,

        @Positive
        long guestExpiration
) {}
