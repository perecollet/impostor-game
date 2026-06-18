package com.impostorgame.game.domain.port.in;

import java.util.List;

public record RoomResponse(
        String roomCode,
        String phase,
        List<PlayerResponse> players
) {
}
