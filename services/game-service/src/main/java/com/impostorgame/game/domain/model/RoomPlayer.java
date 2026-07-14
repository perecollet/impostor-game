package com.impostorgame.game.domain.model;

import com.impostorgame.game.domain.exception.InvalidRoomPlayerException;

public record RoomPlayer(
        PlayerId id,
        String displayName,
        boolean isHost,
        boolean isGuest
) {
    public RoomPlayer {
        if (id == null) throw new InvalidRoomPlayerException("id must not be null");
        if (displayName == null || displayName.isBlank()) throw new InvalidRoomPlayerException("displayName must not be null  or blank");
    }

    public static RoomPlayer of(PlayerId id, String displayName, boolean isHost, boolean isGuest) {
        return new RoomPlayer(id, displayName, isHost, isGuest);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof RoomPlayer roomPlayer) && roomPlayer.id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
