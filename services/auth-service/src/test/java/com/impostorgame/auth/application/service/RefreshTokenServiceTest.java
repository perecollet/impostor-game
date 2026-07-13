package com.impostorgame.auth.application.service;

import com.impostorgame.auth.application.dto.AuthResponse;
import com.impostorgame.auth.application.dto.RefreshTokenRequest;
import com.impostorgame.auth.application.security.TokenHasher;
import com.impostorgame.auth.domain.exception.InvalidTokenException;
import com.impostorgame.auth.domain.model.RefreshToken;
import com.impostorgame.auth.domain.model.Role;
import com.impostorgame.auth.domain.model.User;
import com.impostorgame.auth.domain.port.out.JwtPort;
import com.impostorgame.auth.domain.port.out.RefreshTokenRepository;
import com.impostorgame.auth.domain.port.out.UserRepository;
import com.impostorgame.auth.infrastructure.config.JwtProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class RefreshTokenServiceTest {

    private static final Duration EXPIRATION = Duration.ofHours(24);
    private static final Duration REFRESH_EXPIRATION = Duration.ofDays(30);

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private JwtPort jwtPort;

    private RefreshTokenService refreshTokenService;

    private final JwtProperties jwtProperties = new JwtProperties(
            new ByteArrayResource("private".getBytes()),
            new ByteArrayResource("public".getBytes()),
            "auth-key-1",
            EXPIRATION,
            Duration.ofHours(4),
            REFRESH_EXPIRATION
    );

    private RefreshTokenService service() {
        return new RefreshTokenService(refreshTokenRepository, jwtPort, userRepository, jwtProperties);
    }

    @Test
    void refresh_rotatesTokenAndReturnsNewJwt() {
        UUID userId = UUID.randomUUID();
        String plain = "old-token";
        Instant now = Instant.now();

        User user = User.restore(userId, "user@example.com", "hashed", "Alice", Role.USER, now);
        RefreshToken existing = RefreshToken.restore(
                UUID.randomUUID(), userId, TokenHasher.sha256(plain),
                now.plusSeconds(3600), now);

        when(refreshTokenRepository.findByTokenHash(TokenHasher.sha256(plain))).thenReturn(Optional.of(existing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtPort.generateToken(userId, "Alice", Role.USER)).thenReturn("new-jwt");

        AuthResponse response = service().refresh(new RefreshTokenRequest(plain));

        assertThat(response.token()).isEqualTo("new-jwt");
        assertThat(response.refreshToken()).isNotNull();
        assertThat(response.expiresIn()).isEqualTo(EXPIRATION.toMillis());
        verify(refreshTokenRepository).delete(existing);
    }

    @Test
    void refresh_persistsHashAndReturnsPlainToken() {
        UUID userId = UUID.randomUUID();
        String plain = "old-token";
        Instant now = Instant.now();

        User user = User.restore(userId, "user@example.com", "hashed", "Alice", Role.USER, now);
        RefreshToken existing = RefreshToken.restore(
                UUID.randomUUID(), userId, TokenHasher.sha256(plain),
                now.plusSeconds(3600), now);

        when(refreshTokenRepository.findByTokenHash(TokenHasher.sha256(plain))).thenReturn(Optional.of(existing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtPort.generateToken(userId, "Alice", Role.USER)).thenReturn("new-jwt");

        AuthResponse response = service().refresh(new RefreshTokenRequest(plain));

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken persisted = captor.getValue();

        assertThat(persisted.tokenHash()).isEqualTo(TokenHasher.sha256(response.refreshToken()));
        assertThat(persisted.tokenHash()).isNotEqualTo(response.refreshToken());
        assertThat(persisted.userId()).isEqualTo(userId);
    }

    @Test
    void refresh_throwsInvalidToken_whenTokenNotFound() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());
        RefreshTokenService target = service();

        assertThatThrownBy(() -> target.refresh(new RefreshTokenRequest("bad-token")))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refresh_throwsAndDeletesToken_whenExpired() {
        UUID userId = UUID.randomUUID();
        String plain = "expired-token";
        Instant now = Instant.now();

        RefreshToken expired = RefreshToken.restore(
                UUID.randomUUID(), userId, TokenHasher.sha256(plain),
                now.minusSeconds(3600), now.minusSeconds(7200));

        when(refreshTokenRepository.findByTokenHash(TokenHasher.sha256(plain))).thenReturn(Optional.of(expired));
        RefreshTokenService target = service();

        assertThatThrownBy(() -> target.refresh(new RefreshTokenRequest(plain)))
                .isInstanceOf(InvalidTokenException.class);

        verify(refreshTokenRepository).delete(expired);
    }
}