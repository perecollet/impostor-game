package com.impostorgame.auth.application.service;

import com.impostorgame.auth.TestJwtProperties;
import com.impostorgame.auth.application.dto.AuthResponse;
import com.impostorgame.auth.application.dto.GuestRequest;
import com.impostorgame.auth.domain.model.Role;
import com.impostorgame.auth.domain.port.out.GuestNamePort;
import com.impostorgame.auth.domain.port.out.JwtPort;
import com.impostorgame.auth.infrastructure.config.JwtProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class GuestServiceTest {

    @Mock private JwtPort jwtPort;
    @Mock private GuestNamePort guestNamePort;

    private final JwtProperties jwtProperties = TestJwtProperties.create();

    private GuestService service() {
        return new GuestService(jwtPort, guestNamePort, jwtProperties);
    }

    @Test
    void guest_usesProvidedDisplayName() {
        when(jwtPort.generateToken(any(), eq("SpecificName"), eq(Role.GUEST))).thenReturn("guest-jwt");

        AuthResponse response = service().guest(new GuestRequest("SpecificName"));

        assertThat(response.token()).isEqualTo("guest-jwt");
        assertThat(response.displayName()).isEqualTo("SpecificName");
        assertThat(response.refreshToken()).isNull();
        assertThat(response.role()).isEqualTo("GUEST");
        assertThat(response.expiresIn()).isEqualTo(14400000L);
        verify(guestNamePort, never()).generate();
    }

    @Test
    void guest_generatesRandomName_whenNoneProvided() {
        when(guestNamePort.generate()).thenReturn("RandomName#1234");
        when(jwtPort.generateToken(any(), eq("RandomName#1234"), eq(Role.GUEST))).thenReturn("guest-jwt");

        AuthResponse response = service().guest(new GuestRequest(null));

        assertThat(response.displayName()).isEqualTo("RandomName#1234");
        assertThat(response.refreshToken()).isNull();
        verify(guestNamePort).generate();
    }

    @Test
    void guest_generatesRandomName_whenDisplayNameIsBlank() {
        when(guestNamePort.generate()).thenReturn("Anonymous123");
        when(jwtPort.generateToken(any(UUID.class), eq("Anonymous123"), eq(Role.GUEST))).thenReturn("jwt");

        AuthResponse response = service().guest(new GuestRequest("   "));

        assertThat(response.displayName()).isEqualTo("Anonymous123");
    }
}
