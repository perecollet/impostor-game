package com.impostorgame.auth.application.service;

import com.impostorgame.auth.application.dto.AuthResponse;
import com.impostorgame.auth.application.dto.RegisterRequest;
import com.impostorgame.auth.application.security.TokenHasher;
import com.impostorgame.auth.domain.exception.UserAlreadyExistsException;
import com.impostorgame.auth.domain.model.RefreshToken;
import com.impostorgame.auth.domain.model.Role;
import com.impostorgame.auth.domain.model.User;
import com.impostorgame.auth.domain.port.in.RegisterUseCase;
import com.impostorgame.auth.domain.port.out.JwtPort;
import com.impostorgame.auth.domain.port.out.RefreshTokenRepository;
import com.impostorgame.auth.domain.port.out.UserRepository;
import com.impostorgame.auth.infrastructure.config.JwtProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RegisterService implements RegisterUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtPort jwtPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    public RegisterService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                           JwtPort jwtPort, PasswordEncoder passwordEncoder, JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtPort = jwtPort;
        this.passwordEncoder = passwordEncoder;
        this.jwtProperties = jwtProperties;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException(request.email());
        }

        User user = User.create(request.email(), passwordEncoder.encode(request.password()), request.displayName(), Role.USER);

        User saved = userRepository.save(user);
        String jwt = jwtPort.generateToken(saved.id(), saved.displayName(), saved.role());
        String refreshToken = saveRefreshToken(saved.id());

        return new AuthResponse(jwt, refreshToken, saved.id(), saved.displayName(), saved.role().name(), jwtProperties.expiration().toMillis());
    }

    private String saveRefreshToken(UUID userId) {
        String plain = UUID.randomUUID().toString();
        refreshTokenRepository.save(RefreshToken.create(userId, TokenHasher.sha256(plain), jwtProperties.refreshExpiration()));
        return plain;
    }
}
