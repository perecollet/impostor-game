package com.impostorgame.game.domain.model;

public record GuestPlayer(String id, String displayName) implements PlayerContext{
    @Override
    public boolean isGuest() {
        return true;
    }
}
