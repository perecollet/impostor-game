package com.impostorgame.game.domain.model;

import com.impostorgame.game.domain.exception.InvalidPlayerIdException;

public record PlayerId(String value) {

    public PlayerId {
        if (value == null || value.isBlank()) throw new InvalidPlayerIdException("PlayerId must be not null or blank");
    }

    public static PlayerId of(String value) {
        return new PlayerId(value);
    }
}
