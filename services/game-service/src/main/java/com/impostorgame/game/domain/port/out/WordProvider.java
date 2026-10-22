package com.impostorgame.game.domain.port.out;

import com.impostorgame.game.domain.exception.WordPairUnavailableException;
import com.impostorgame.game.domain.model.Word;
import com.impostorgame.game.domain.model.WordPair;

import java.util.Set;

public interface WordProvider {

    /**
     * Obtains a new word pair.
     *
     * <p>Neither {@code word} nor {@code hint} of the returned pair is
     * contained in {@code usedWords}, in either role.
     *
     * @param usedWords words that must not appear in the returned pair;
     *                  never {@code null}, may be empty
     * @return a valid word pair, never {@code null}
     * @throws WordPairUnavailableException if no pair satisfying these
     *         constraints can be obtained
     */
    WordPair obtainWords(Set<Word> usedWords);
}
