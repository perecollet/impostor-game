package com.impostorgame.auth.application.service;

import com.impostorgame.auth.application.dto.AuthResponse;
import com.impostorgame.auth.application.dto.RefreshTokenRequest;
import com.impostorgame.auth.domain.exception.InvalidTokenException;
import com.impostorgame.auth.domain.model.RefreshToken;
import com.impostorgame.auth.domain.model.Role;
import com.impostorgame.auth.domain.model.User;
import com.impostorgame.auth.domain.port.in.RefreshTokenUseCase;
import com.impostorgame.auth.domain.port.out.JwtPort;
import com.impostorgame.auth.domain.port.out.RefreshTokenRepository;
import com.impostorgame.auth.domain.port.out.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService implements RefreshTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtPort jwtPort;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtPort jwtPort, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtPort = jwtPort;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken existing = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (existing.isExpired()) {
            refreshTokenRepository.delete(existing);
            throw new InvalidTokenException("Refresh token expired");
        }

        User user = userRepository.findById(existing.getUserId())
                .orElseThrow(() -> new InvalidTokenException("User not found"));

        String newJwt = jwtPort.generateToken(user.id(), user.displayName(), Role.USER);
        String newRefreshToken = saveRefreshToken(user.id());
        refreshTokenRepository.delete(existing);

        return new AuthResponse(newJwt, newRefreshToken, user.id(), user.displayName(), Role.USER.name(), 86400000L);
    }

    private String saveRefreshToken(UUID userId) {
        Instant now = Instant.now();
        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .token(UUID.randomUUID().toString())
                .expiresAt(now.plusSeconds(2592000))
                .createdAt(now)
                .build();
        return refreshTokenRepository.save(token).getToken();
    }
}
