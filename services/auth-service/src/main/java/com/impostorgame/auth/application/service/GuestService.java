package com.impostorgame.auth.application.service;

import com.impostorgame.auth.application.dto.AuthResponse;
import com.impostorgame.auth.application.dto.GuestRequest;
import com.impostorgame.auth.domain.model.Role;
import com.impostorgame.auth.domain.port.in.GuestUseCase;
import com.impostorgame.auth.domain.port.out.GuestNamePort;
import com.impostorgame.auth.domain.port.out.JwtPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuestService implements GuestUseCase {

    private final JwtPort jwtPort;
    private final GuestNamePort guestNamePort;

    @Override
    public AuthResponse guest(GuestRequest request) {
        String displayName = request.displayName() != null
                ? request.displayName()
                : guestNamePort.generate();

        UUID guestId = UUID.randomUUID();
        String jwt = jwtPort.generateToken(guestId, displayName, Role.GUEST);

        return new AuthResponse(jwt, null, guestId, displayName, Role.GUEST.name(), 14400000L);
    }
}
