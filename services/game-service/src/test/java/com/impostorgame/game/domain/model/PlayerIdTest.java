package com.impostorgame.game.domain.model;

import com.impostorgame.game.domain.exception.InvalidPlayerIdException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class PlayerIdTest {

    @Test
    void shouldCreateFromString() {
        PlayerId id = PlayerId.of("user-123");
        assertThat(id.value()).isEqualTo("user-123");
    }

    @Test
    void shouldRejectNullValue() {
        assertThatThrownBy(() -> PlayerId.of(null))
                .isInstanceOf(InvalidPlayerIdException.class);
    }

    @Test
    void shouldRejectBlankValue() {
        assertThatThrownBy(() -> PlayerId.of("   "))
                .isInstanceOf(InvalidPlayerIdException.class);
    }

    @Test
    void shouldBeEqualWhenSameValue() {
        PlayerId a = PlayerId.of("user-123");
        PlayerId b = PlayerId.of("user-123");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenDifferentValue() {
        assertThat(PlayerId.of("user-1")).isNotEqualTo(PlayerId.of("user-2"));
    }
}