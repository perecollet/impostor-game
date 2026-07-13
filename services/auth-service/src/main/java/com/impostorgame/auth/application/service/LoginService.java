package com.impostorgame.auth.application.service;

import com.impostorgame.auth.application.dto.AuthResponse;
import com.impostorgame.auth.application.dto.LoginRequest;
import com.impostorgame.auth.domain.exception.InvalidCredentialsException;
import com.impostorgame.auth.domain.model.User;
import com.impostorgame.auth.domain.port.in.LoginUseCase;
import com.impostorgame.auth.domain.port.out.JwtPort;
import com.impostorgame.auth.domain.port.out.UserRepository;
import com.impostorgame.auth.infrastructure.config.JwtProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService implements LoginUseCase {

    private final UserRepository userRepository;
    private final JwtPort jwtPort;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenIssuer refreshTokenIssuer;


    public LoginService(UserRepository userRepository, JwtPort jwtPort, PasswordEncoder passwordEncoder,
                        JwtProperties jwtProperties, RefreshTokenIssuer refreshTokenIssuer) {
        this.userRepository = userRepository;
        this.jwtPort = jwtPort;
        this.passwordEncoder = passwordEncoder;
        this.jwtProperties = jwtProperties;
        this.refreshTokenIssuer = refreshTokenIssuer;
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
        String refreshToken = refreshTokenIssuer.issue(user.id());

        return new AuthResponse(jwt, refreshToken, user.id(), user.displayName(), user.role().name(), jwtProperties.expiration().toMillis());
    }
}
