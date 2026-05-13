package com.impostorgame.auth.application.service;

import com.impostorgame.auth.application.dto.AuthResponse;
import com.impostorgame.auth.application.dto.LoginRequest;
import com.impostorgame.auth.domain.exception.InvalidCredentialsException;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class LoginServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtPort jwtPort;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private LoginService loginService;

    @Test
    void login_returnsTokensForValidCredentials() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId).email("user@example.com").password("hashed")
                .displayName("Alice").role(Role.USER).createdAt(Instant.now())
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtPort.generateToken(userId, "Alice", Role.USER)).thenReturn("jwt-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = loginService.login(new LoginRequest("user@example.com", "password123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.displayName()).isEqualTo("Alice");
        assertThat(response.refreshToken()).isNotNull();
    }

    @Test
    void login_throwsInvalidCredentials_whenUserNotFound() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.login(new LoginRequest("unknown@example.com", "pass")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_throwsInvalidCredentials_whenPasswordWrong() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId).email("user@example.com").password("hashed")
                .displayName("Alice").role(Role.USER).createdAt(Instant.now())
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> loginService.login(new LoginRequest("user@example.com", "wrongpass")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
