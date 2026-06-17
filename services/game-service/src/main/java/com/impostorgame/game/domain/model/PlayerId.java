package com.impostorgame.game.domain.model;

public record PlayerId(String value) {

    public PlayerId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("PlayerId must be not null or blank");
    }

    public static PlayerId of(String value) {
        return new PlayerId(value);
    }
}
