package com.impostorgame.game.domain.model;

public record RegisteredPlayer (String id, String displayName) implements PlayerContext{
    @Override
    public boolean isGuest() {
        return false;
    }
}
