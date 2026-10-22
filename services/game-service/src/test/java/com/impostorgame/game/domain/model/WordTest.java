package com.impostorgame.game.domain.model;

import com.impostorgame.game.domain.exception.InvalidWordException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class WordTest {

    @Test
    void shouldNormalizeToLowercase() {
        Word word = new Word("GaTo");

        assertThat(word.value()).isEqualTo("gato");
    }

    @Test
    void shouldTrimSurroundingWhitespace() {
        Word word = new Word("  gato  ");

        assertThat(word.value()).isEqualTo("gato");
    }

    @Test
    void shouldPreserveAccentsWhenNormalizing() {
        Word word = new Word("LEÓN");

        assertThat(word.value()).isEqualTo("león");
    }

    @Test
    void shouldNormalizeIndependentlyOfDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.of("tr"));

            Word word = new Word("TITLE");

            assertThat(word.value()).isEqualTo("title");
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void shouldBeEqualIgnoringCaseAndSurroundingWhitespace() {
        Word upper = new Word("GATO");
        Word padded = new Word("  gato ");

        assertThat(upper).isEqualTo(padded).hasSameHashCodeAs(padded);
    }

    @Test
    void shouldDeduplicateInSetIgnoringCase() {
        Set<Word> words = new HashSet<>();

        words.add(new Word("Gato"));
        words.add(new Word("gato"));
        words.add(new Word("GATO"));

        assertThat(words).hasSize(1);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t", "\n"})
    void shouldRejectNullOrBlank(String value) {
        assertThatThrownBy(() -> new Word(value))
                .isInstanceOf(InvalidWordException.class);
    }

    @Test
    void stripsUnicodeWhitespace() {
        Word word = new Word("\u2003gato\u2003");

        assertThat(word.value()).isEqualTo("gato");
    }
}