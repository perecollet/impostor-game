package com.impostorgame.game.domain.model;

import com.impostorgame.game.domain.exception.InvalidWordException;

import java.util.Locale;

public record Word(String value) {

    public Word {
        if (value == null || value.isBlank()) {
            throw new InvalidWordException("The value must not be blank or null");
        }

       value = value.strip().toLowerCase(Locale.ROOT);
    }
}
