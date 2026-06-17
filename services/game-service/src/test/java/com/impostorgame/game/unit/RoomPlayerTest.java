package com.impostorgame.game.unit;

import com.impostorgame.game.domain.model.PlayerId;
import com.impostorgame.game.domain.model.RoomPlayer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

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