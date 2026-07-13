package com.impostorgame.auth.application.service;

import com.impostorgame.auth.application.dto.AuthResponse;
import com.impostorgame.auth.application.dto.RefreshTokenRequest;
import com.impostorgame.auth.application.security.TokenHasher;
import com.impostorgame.auth.domain.exception.InvalidTokenException;
import com.impostorgame.auth.domain.model.RefreshToken;
import com.impostorgame.auth.domain.model.User;
import com.impostorgame.auth.domain.port.in.RefreshTokenUseCase;
import com.impostorgame.auth.domain.port.out.JwtPort;
import com.impostorgame.auth.domain.port.out.RefreshTokenRepository;
import com.impostorgame.auth.domain.port.out.UserRepository;
import com.impostorgame.auth.infrastructure.config.JwtProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RefreshTokenService implements RefreshTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtPort jwtPort;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtPort jwtPort,
                               UserRepository userRepository, JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtPort = jwtPort;
        this.userRepository = userRepository;
        this.jwtProperties = jwtProperties;
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken existing = refreshTokenRepository.findByTokenHash(TokenHasher.sha256(request.refreshToken()))
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (existing.isExpired()) {
            refreshTokenRepository.delete(existing);
            throw new InvalidTokenException("Refresh token expired");
        }

        User user = userRepository.findById(existing.userId())
                .orElseThrow(() -> new InvalidTokenException("User not found"));

        String newJwt = jwtPort.generateToken(user.id(), user.displayName(), user.role());
        String newRefreshToken = saveRefreshToken(user.id());
        refreshTokenRepository.delete(existing);

        return new AuthResponse(newJwt, newRefreshToken, user.id(), user.displayName(), user.role().name(), jwtProperties.expiration().toMillis());
    }

    private String saveRefreshToken(UUID userId) {
        String plain = UUID.randomUUID().toString();
        refreshTokenRepository.save(RefreshToken.create(userId, TokenHasher.sha256(plain), jwtProperties.refreshExpiration()));
        return plain;
    }
}
