package com.impostorgame.game.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class RoomPlayerTest {

    @Test
    void shouldCreateRoomPlayer() {
        RoomPlayer player = RoomPlayer.of(PlayerId.of("user-1"), "Alice", true, false);
        assertThat(player.id()).isEqualTo(PlayerId.of("user-1"));
        assertThat(player.displayName()).isEqualTo("Alice");
        assertThat(player.isHost()).isTrue();
        assertThat(player.isGuest()).isFalse();
    }

    @Test
    void shouldRejectNullId() {
        assertThatThrownBy(() -> RoomPlayer.of(null, "Alice", false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectBlankDisplayName() {
        assertThatThrownBy(() -> RoomPlayer.of(PlayerId.of("user-1"), "  ", false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldBeEqualWhenSameId() {
        RoomPlayer a = RoomPlayer.of(PlayerId.of("user-1"), "Alice", true, false);
        RoomPlayer b = RoomPlayer.of(PlayerId.of("user-1"), "Alicia", false, true);
        assertThat(a).isEqualTo(b);
    }
}