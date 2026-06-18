package com.impostorgame.auth.application.service;

import com.impostorgame.auth.application.dto.AuthResponse;
import com.impostorgame.auth.application.dto.RegisterRequest;
import com.impostorgame.auth.domain.exception.UserAlreadyExistsException;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class RegisterServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtPort jwtPort;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private RegisterService registerService;

    @Test
    void register_createsUserAndReturnsTokens() {
        UUID userId = UUID.randomUUID();
        var request = new RegisterRequest("user@example.com", "password123", "Alice");

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return User.builder()
                    .id(userId).email(u.getEmail()).password(u.getPassword())
                    .displayName(u.getDisplayName()).role(u.getRole()).createdAt(u.getCreatedAt())
                    .build();
        });
        when(jwtPort.generateToken(userId, "Alice", Role.USER)).thenReturn("jwt-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = registerService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.refreshToken()).isNotNull();
        assertThat(response.displayName()).isEqualTo("Alice");
        assertThat(response.role()).isEqualTo("USER");
        assertThat(response.playerId()).isEqualTo(userId);
    }

    @Test
    void register_throwsUserAlreadyExistsException_whenEmailTaken() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> registerService.register(
                new RegisterRequest("user@example.com", "password123", "Alice")))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }
}
