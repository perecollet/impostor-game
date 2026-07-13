package com.impostorgame.auth.application.service;

import com.impostorgame.auth.application.dto.AuthResponse;
import com.impostorgame.auth.application.dto.RegisterRequest;
import com.impostorgame.auth.domain.exception.UserAlreadyExistsException;
import com.impostorgame.auth.domain.model.Role;
import com.impostorgame.auth.domain.model.User;
import com.impostorgame.auth.domain.port.in.RegisterUseCase;
import com.impostorgame.auth.domain.port.out.JwtPort;
import com.impostorgame.auth.domain.port.out.UserRepository;
import com.impostorgame.auth.infrastructure.config.JwtProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterService implements RegisterUseCase {

    private final UserRepository userRepository;
    private final JwtPort jwtPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;
    private final RefreshTokenIssuer refreshTokenIssuer;

    public RegisterService(UserRepository userRepository, JwtPort jwtPort, PasswordEncoder passwordEncoder,
                           JwtProperties jwtProperties, RefreshTokenIssuer refreshTokenIssuer) {
        this.userRepository = userRepository;
        this.jwtPort = jwtPort;
        this.passwordEncoder = passwordEncoder;
        this.jwtProperties = jwtProperties;
        this.refreshTokenIssuer = refreshTokenIssuer;
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
        String refreshToken = refreshTokenIssuer.issue(saved.id());

        return new AuthResponse(jwt, refreshToken, saved.id(), saved.displayName(), saved.role().name(), jwtProperties.expiration().toMillis());
    }
}
