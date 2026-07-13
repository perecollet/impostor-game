package com.impostorgame.auth.application.service;

import com.impostorgame.auth.application.dto.AuthResponse;
import com.impostorgame.auth.application.dto.GuestRequest;
import com.impostorgame.auth.domain.model.Role;
import com.impostorgame.auth.domain.port.in.GuestUseCase;
import com.impostorgame.auth.domain.port.out.GuestNamePort;
import com.impostorgame.auth.domain.port.out.JwtPort;
import com.impostorgame.auth.infrastructure.config.JwtProperties;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GuestService implements GuestUseCase {

    private final JwtPort jwtPort;
    private final JwtProperties jwtProperties;
    private final GuestNamePort guestNamePort;

    public GuestService(JwtPort jwtPort, GuestNamePort guestNamePort, JwtProperties jwtProperties) {
        this.jwtPort = jwtPort;
        this.guestNamePort = guestNamePort;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public AuthResponse guest(GuestRequest request) {
        String displayName = request.displayName() != null
                ? request.displayName()
                : guestNamePort.generate();

        UUID guestId = UUID.randomUUID();
        String jwt = jwtPort.generateToken(guestId, displayName, Role.GUEST);

        return new AuthResponse(jwt, null, guestId, displayName, Role.GUEST.name(), jwtProperties.guestExpiration().toMillis());
    }
}
