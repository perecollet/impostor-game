package com.impostorgame.auth.application.service;

import com.impostorgame.auth.application.security.TokenHasher;
import com.impostorgame.auth.domain.model.RefreshToken;
import com.impostorgame.auth.domain.port.out.RefreshTokenRepository;
import com.impostorgame.auth.infrastructure.config.JwtProperties;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RefreshTokenIssuer {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public RefreshTokenIssuer(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    public String issue(UUID userId) {
        String plain = UUID.randomUUID().toString();
        refreshTokenRepository.save(RefreshToken.create(userId, TokenHasher.sha256(plain), jwtProperties.refreshExpiration()));
        return plain;
    }
}
