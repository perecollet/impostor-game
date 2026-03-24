package com.impostorgame.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String token,
        String refreshToken,  // null for guests
        UUID playerId,
        String displayName,
        String role,
        long expiresIn
) {}