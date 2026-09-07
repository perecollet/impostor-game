package com.impostorgame.game.domain.model;

import com.impostorgame.game.domain.exception.InvalidWordPairException;

public record WordPair(String word, String hint) {

    public WordPair {
        if (word == null || word.isBlank()) {
            throw new InvalidWordPairException("The word must not be null or blank");
        }

        if (hint == null || hint.isBlank()) {
            throw new InvalidWordPairException("The hint must not be null or blank");
        }

        word = word.trim();
        hint = hint.trim();

        if (word.equalsIgnoreCase(hint)) {
            throw new InvalidWordPairException("The word and hint can not be the same");
        }
    }
}
