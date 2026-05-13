package com.impostorgame.auth.application.service;

import com.impostorgame.auth.application.dto.AuthResponse;
import com.impostorgame.auth.application.dto.RegisterRequest;
import com.impostorgame.auth.domain.exception.UserAlreadyExistsException;
import com.impostorgame.auth.domain.model.RefreshToken;
import com.impostorgame.auth.domain.model.Role;
import com.impostorgame.auth.domain.model.User;
import com.impostorgame.auth.domain.port.in.RegisterUseCase;
import com.impostorgame.auth.domain.port.out.JwtPort;
import com.impostorgame.auth.domain.port.out.RefreshTokenRepository;
import com.impostorgame.auth.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterService implements RegisterUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtPort jwtPort;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException(request.email());
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .role(Role.USER)
                .createdAt(Instant.now())
                .build();

        User saved = userRepository.save(user);
        String jwt = jwtPort.generateToken(saved.getId(), saved.getDisplayName(), Role.USER);
        String refreshToken = saveRefreshToken(saved.getId());

        return new AuthResponse(jwt, refreshToken, saved.getId(), saved.getDisplayName(), Role.USER.name(), 86400000L);
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
