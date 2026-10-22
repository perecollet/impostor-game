package com.impostorgame.game.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.impostorgame.game.domain.exception.InvalidWordPairException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class WordPairTest {

    @Test
    void createsPairFromDistinctWords() {
        Word word = new Word("Gato");
        Word hint = new Word("Felino");

        WordPair pair = new WordPair(word, hint);

        assertThat(pair.word()).isEqualTo(word);
        assertThat(pair.hint()).isEqualTo(hint);
    }

    @Test
    void rejectsNullWord() {
        Word hint = new Word("Felino");

        assertThatThrownBy(() -> new WordPair(null, hint))
                .isInstanceOf(InvalidWordPairException.class);
    }

    @Test
    void rejectsNullHint() {
        Word word = new Word("Gato");

        assertThatThrownBy(() -> new WordPair(word, null))
                .isInstanceOf(InvalidWordPairException.class);
    }

    @Test
    void rejectsWordAndHintEqualIgnoringCase() {
        Word word = new Word("Gato");
        Word hint = new Word("gato");

        assertThatThrownBy(() -> new WordPair(word, hint))
                .isInstanceOf(InvalidWordPairException.class);
    }

    @Test
    void rejectsWordAndHintEqualAfterTrim() {
        Word word = new Word("  Gato  ");
        Word hint = new Word("gato");

        assertThatThrownBy(() -> new WordPair(word, hint))
                .isInstanceOf(InvalidWordPairException.class);
    }
}