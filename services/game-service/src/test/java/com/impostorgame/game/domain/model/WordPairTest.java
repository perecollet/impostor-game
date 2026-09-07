package com.impostorgame.game.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.impostorgame.game.domain.exception.InvalidWordPairException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("unit")
class WordPairTest {

    @Test
    void createsPairPreservingOriginalCase() {
        WordPair pair = new WordPair("Gato", "Felino");

        assertThat(pair.word()).isEqualTo("Gato");
        assertThat(pair.hint()).isEqualTo("Felino");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t"})
    void rejectsBlankWord(String blank) {
        assertThatThrownBy(() -> new WordPair(blank, "Felino"))
                .isInstanceOf(InvalidWordPairException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t"})
    void rejectsBlankHint(String blank) {
        assertThatThrownBy(() -> new WordPair("Gato", blank))
                .isInstanceOf(InvalidWordPairException.class);
    }

    @Test
    void rejectsWordAndHintEqualIgnoringCase() {
        assertThatThrownBy(() -> new WordPair("Gato", "gato"))
                .isInstanceOf(InvalidWordPairException.class);
    }

    @Test
    void rejectsWordAndHintEqualAfterTrim() {
        assertThatThrownBy(() -> new WordPair("  Gato  ", "gato"))
                .isInstanceOf(InvalidWordPairException.class);
    }

    @Test
    void trimsSurroundingWhitespaceKeepingInnerText() {
        WordPair pair = new WordPair("  Gato  ", "  Felino  ");

        assertThat(pair.word()).isEqualTo("Gato");
        assertThat(pair.hint()).isEqualTo("Felino");
    }
}