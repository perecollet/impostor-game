package com.impostorgame.auth.application.service;

import com.impostorgame.auth.application.dto.AuthResponse;
import com.impostorgame.auth.application.dto.RefreshTokenRequest;
import com.impostorgame.auth.domain.exception.InvalidTokenException;
import com.impostorgame.auth.domain.model.RefreshToken;
import com.impostorgame.auth.domain.model.Role;
import com.impostorgame.auth.domain.model.User;
import com.impostorgame.auth.domain.port.out.JwtPort;
import com.impostorgame.auth.domain.port.out.RefreshTokenRepository;
import com.impostorgame.auth.domain.port.out.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private JwtPort jwtPort;

    @InjectMocks private RefreshTokenService refreshTokenService;

    @Test
    void refresh_rotatesTokenAndReturnsNewJwt() {
        UUID userId = UUID.randomUUID();
        User user = User.restore(userId, "user@example.com", "hashed", "Alice", Role.USER, Instant.now());

        RefreshToken existing = RefreshToken.builder()
                .id(UUID.randomUUID()).userId(userId).token("old-token")
                .expiresAt(Instant.now().plusSeconds(3600)).createdAt(Instant.now())
                .build();

        when(refreshTokenRepository.findByToken("old-token")).thenReturn(Optional.of(existing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtPort.generateToken(userId, "Alice", Role.USER)).thenReturn("new-jwt");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = refreshTokenService.refresh(new RefreshTokenRequest("old-token"));

        assertThat(response.token()).isEqualTo("new-jwt");
        assertThat(response.refreshToken()).isNotNull();
        verify(refreshTokenRepository).delete(existing);
    }

    @Test
    void refresh_throwsInvalidToken_whenTokenNotFound() {
        when(refreshTokenRepository.findByToken(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.refresh(new RefreshTokenRequest("bad-token")))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refresh_throwsAndDeletesToken_whenExpired() {
        UUID userId = UUID.randomUUID();
        RefreshToken expired = RefreshToken.builder()
                .id(UUID.randomUUID()).userId(userId).token("expired-token")
                .expiresAt(Instant.now().minusSeconds(3600)).createdAt(Instant.now().minusSeconds(7200))
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.refresh(new RefreshTokenRequest("expired-token")))
                .isInstanceOf(InvalidTokenException.class);

        verify(refreshTokenRepository).delete(expired);
    }
}
