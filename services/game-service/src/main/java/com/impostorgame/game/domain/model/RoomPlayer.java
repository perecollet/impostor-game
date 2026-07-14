package com.impostorgame.game.domain.model;

import com.impostorgame.game.domain.exception.InvalidRoomPlayerException;

public class RoomPlayer {

    private final PlayerId id;
    private final String displayName;
    private final boolean isHost;
    private final boolean isGuest;

    private RoomPlayer(PlayerId id, String displayName, boolean isHost, boolean isGuest) {
        if (id == null) throw new InvalidRoomPlayerException("id must not be null");
        if (displayName == null || displayName.isBlank()) throw new InvalidRoomPlayerException("displayName must not be null  or blank");

        this.id = id;
        this.displayName = displayName;
        this.isHost = isHost;
        this.isGuest = isGuest;
    }

    public static RoomPlayer of(PlayerId id, String displayName, boolean isHost, boolean isGuest) {
        return new RoomPlayer(id, displayName, isHost, isGuest);
    }

    public PlayerId id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isHost() {
        return isHost;
    }

    public boolean isGuest() {
        return isGuest;
    }
}
