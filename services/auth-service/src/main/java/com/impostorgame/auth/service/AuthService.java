package com.impostorgame.auth.service;

import com.impostorgame.auth.domain.RefreshToken;
import com.impostorgame.auth.domain.Role;
import com.impostorgame.auth.domain.User;
import com.impostorgame.auth.dto.*;
import com.impostorgame.auth.exception.InvalidCredentialsException;
import com.impostorgame.auth.exception.InvalidTokenException;
import com.impostorgame.auth.exception.UserAlreadyExistsException;
import com.impostorgame.auth.repository.RefreshTokenRepository;
import com.impostorgame.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final GuestNameGenerator guestNameGenerator;
    private final PasswordEncoder passwordEncoder;

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
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(
                user.getId(), user.getDisplayName(), Role.USER);
        String refreshToken = generateRefreshToken(user);

        return new AuthResponse(
                token,
                refreshToken,
                user.getId(),
                user.getDisplayName(),
                Role.USER.name(),
                86400000L
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(
                user.getId(), user.getDisplayName(), Role.USER);
        String refreshToken = generateRefreshToken(user);

        return new AuthResponse(
                token,
                refreshToken,
                user.getId(),
                user.getDisplayName(),
                Role.USER.name(),
                86400000L
        );
    }

    public AuthResponse guest(GuestRequest request) {
        String displayName = request.displayName() != null
                ? request.displayName()
                : guestNameGenerator.generate();

        UUID guestId = UUID.randomUUID();
        String token = jwtService.generateToken(guestId, displayName, Role.GUEST);

        return new AuthResponse(
                token,
                null,
                guestId,
                displayName,
                Role.GUEST.name(),
                14400000L
        );
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(request.refreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidTokenException("Refresh token expired");
        }

        User user = refreshToken.getUser();
        String newToken = jwtService.generateToken(
                user.getId(), user.getDisplayName(), Role.USER);
        String newRefreshToken = generateRefreshToken(user);

        refreshTokenRepository.delete(refreshToken);

        return new AuthResponse(
                newToken,
                newRefreshToken,
                user.getId(),
                user.getDisplayName(),
                Role.USER.name(),
                86400000L
        );
    }

    private String generateRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plusSeconds(2592000))
                .build();

        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }

}