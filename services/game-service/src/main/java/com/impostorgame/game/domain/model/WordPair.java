package com.impostorgame.game.domain.model;

import com.impostorgame.game.domain.exception.InvalidWordPairException;

public record WordPair(Word word, Word hint) {

    public WordPair {
        if (word == null) {
            throw new InvalidWordPairException("The word must not be null");
        }

        if (hint == null) {
            throw new InvalidWordPairException("The hint must not be null");
        }

        if (word.equals(hint)) {
            throw new InvalidWordPairException("The word and hint can not be the same");
        }
    }
}
