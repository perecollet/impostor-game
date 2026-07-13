package com.impostorgame.auth.application.service;

import com.impostorgame.auth.application.dto.AuthResponse;
import com.impostorgame.auth.application.dto.LoginRequest;
import com.impostorgame.auth.application.security.TokenHasher;
import com.impostorgame.auth.domain.exception.InvalidCredentialsException;
import com.impostorgame.auth.domain.model.RefreshToken;
import com.impostorgame.auth.domain.model.Role;
import com.impostorgame.auth.domain.model.User;
import com.impostorgame.auth.domain.port.in.LoginUseCase;
import com.impostorgame.auth.domain.port.out.JwtPort;
import com.impostorgame.auth.domain.port.out.RefreshTokenRepository;
import com.impostorgame.auth.domain.port.out.UserRepository;
import com.impostorgame.auth.infrastructure.config.JwtProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LoginService implements LoginUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtPort jwtPort;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;

    public LoginService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                        JwtPort jwtPort, PasswordEncoder passwordEncoder,  JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtPort = jwtPort;
        this.passwordEncoder = passwordEncoder;
        this.jwtProperties = jwtProperties;
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        String jwt = jwtPort.generateToken(user.id(), user.displayName(), user.role());
        String refreshToken = saveRefreshToken(user.id());

        return new AuthResponse(jwt, refreshToken, user.id(), user.displayName(), user.role().name(), jwtProperties.expiration().toMillis());
    }

    private String saveRefreshToken(UUID userId) {
        String plain = UUID.randomUUID().toString();
        refreshTokenRepository.save(RefreshToken.create(userId, TokenHasher.sha256(plain), jwtProperties.refreshExpiration()));
        return plain;
    }
}
