package com.impostorgame.game.domain.model;

import com.impostorgame.game.domain.exception.InvalidRoomPlayerException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class RoomPlayerTest {

    @Test
    void restore_buildsRoomPlayerWithGivenState() {
        RoomPlayer player = RoomPlayer.restore(PlayerId.of("user-1"), "Alice", true, false);

        assertThat(player.id()).isEqualTo(PlayerId.of("user-1"));
        assertThat(player.displayName()).isEqualTo("Alice");
        assertThat(player.isHost()).isTrue();
        assertThat(player.isGuest()).isFalse();
    }

    @Test
    void restore_rejectsNullId() {
        assertThatThrownBy(() -> RoomPlayer.restore(null, "Alice", false, false))
                .isInstanceOf(InvalidRoomPlayerException.class);
    }

    @Test
    void restore_rejectsBlankDisplayName() {
        assertThatThrownBy(() -> RoomPlayer.restore(PlayerId.of("user-1"), "  ", false, false))
                .isInstanceOf(InvalidRoomPlayerException.class);
    }

    @Test
    void host_marksPlayerAsHost() {
        RoomPlayer player = RoomPlayer.host(new RegisteredPlayer("user-1", "Alice"));

        assertThat(player.id()).isEqualTo(PlayerId.of("user-1"));
        assertThat(player.displayName()).isEqualTo("Alice");
        assertThat(player.isHost()).isTrue();
        assertThat(player.isGuest()).isFalse();
    }

    @Test
    void host_marksGuestPlayerAsGuest() {
        RoomPlayer player = RoomPlayer.host(new GuestPlayer("guest-1", "Anon"));

        assertThat(player.isHost()).isTrue();
        assertThat(player.isGuest()).isTrue();
    }
}