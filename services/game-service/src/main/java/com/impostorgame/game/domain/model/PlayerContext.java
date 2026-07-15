package com.impostorgame.game.domain.model;

public sealed interface PlayerContext permits RegisteredPlayer, GuestPlayer {
    String id();
    String displayName();
    boolean isGuest();
}
