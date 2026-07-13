package com.impostorgame.auth.application.service;

import com.impostorgame.auth.TestJwtProperties;
import com.impostorgame.auth.application.dto.AuthResponse;
import com.impostorgame.auth.application.dto.RegisterRequest;
import com.impostorgame.auth.domain.exception.UserAlreadyExistsException;
import com.impostorgame.auth.domain.model.Role;
import com.impostorgame.auth.domain.model.User;
import com.impostorgame.auth.domain.port.out.JwtPort;
import com.impostorgame.auth.domain.port.out.UserRepository;
import com.impostorgame.auth.infrastructure.config.JwtProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class RegisterServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtPort jwtPort;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenIssuer refreshTokenIssuer;

    private final JwtProperties jwtProperties = TestJwtProperties.create();

    private RegisterService service() {
        return new RegisterService(userRepository, jwtPort, passwordEncoder, jwtProperties, refreshTokenIssuer);
    }

    @Test
    void register_createsUserAndReturnsTokens() {
        var request = new RegisterRequest("user@example.com", "password123", "Alice");

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtPort.generateToken(any(UUID.class), eq("Alice"), eq(Role.USER))).thenReturn("jwt-token");
        when(refreshTokenIssuer.issue(any(UUID.class))).thenReturn("plain-refresh-token");

        AuthResponse response = service().register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User persisted = captor.getValue();

        assertThat(persisted.passwordHash()).isEqualTo("hashed");
        assertThat(persisted.email()).isEqualTo("user@example.com");
        assertThat(persisted.displayName()).isEqualTo("Alice");
        assertThat(persisted.role()).isEqualTo(Role.USER);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.refreshToken()).isEqualTo("plain-refresh-token");
        assertThat(response.displayName()).isEqualTo("Alice");
        assertThat(response.role()).isEqualTo("USER");
        assertThat(response.playerId()).isEqualTo(persisted.id());
        assertThat(response.expiresIn()).isEqualTo(TestJwtProperties.EXPIRATION.toMillis());
    }

    @Test
    void register_issuesRefreshTokenForCreatedUser() {
        var request = new RegisterRequest("user@example.com", "password123", "Alice");

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtPort.generateToken(any(UUID.class), eq("Alice"), eq(Role.USER))).thenReturn("jwt-token");
        when(refreshTokenIssuer.issue(any(UUID.class))).thenReturn("plain-refresh-token");

        AuthResponse response = service().register(request);

        verify(refreshTokenIssuer).issue(response.playerId());
    }

    @Test
    void register_throwsUserAlreadyExistsException_whenEmailTaken() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);
        RegisterService target = service();
        var request = new RegisterRequest("user@example.com", "password123", "Alice");

        assertThatThrownBy(() -> target.register(request))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
        verify(refreshTokenIssuer, never()).issue(any());
    }
}