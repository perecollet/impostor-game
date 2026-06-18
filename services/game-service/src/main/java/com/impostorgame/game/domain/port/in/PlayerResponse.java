package com.impostorgame.game.domain.port.in;

public record PlayerResponse(
        String id,
        String displayName,
        boolean isHost,
        boolean isGuest
) {
}
