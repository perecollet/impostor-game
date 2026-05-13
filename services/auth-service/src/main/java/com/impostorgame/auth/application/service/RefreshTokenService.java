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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService implements RefreshTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtPort jwtPort;

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

        String newJwt = jwtPort.generateToken(user.getId(), user.getDisplayName(), Role.USER);
        String newRefreshToken = saveRefreshToken(user.getId());
        refreshTokenRepository.delete(existing);

        return new AuthResponse(newJwt, newRefreshToken, user.getId(), user.getDisplayName(), Role.USER.name(), 86400000L);
    }

    private String saveRefreshToken(UUID userId) {
        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .token(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plusSeconds(2592000))
                .createdAt(Instant.now())
                .build();
        return refreshTokenRepository.save(token).getToken();
    }
}
