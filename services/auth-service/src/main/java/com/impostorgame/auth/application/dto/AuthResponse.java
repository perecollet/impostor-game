package com.impostorgame.auth.application.dto;

import java.util.UUID;

public record AuthResponse(
        String token,
        String refreshToken,
        UUID playerId,
        String displayName,
        String role,
        long expiresIn
) {}
