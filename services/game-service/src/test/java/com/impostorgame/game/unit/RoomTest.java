package com.impostorgame.game.unit;

import com.impostorgame.game.domain.model.GamePhase;
import com.impostorgame.game.domain.model.PlayerId;
import com.impostorgame.game.domain.model.Room;
import com.impostorgame.game.domain.model.RoomCode;
import com.impostorgame.game.domain.model.RoomPlayer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class RoomTest {

    private static RoomPlayer host() {
        return RoomPlayer.of(PlayerId.of("host-1"), "Alice", true, false);
    }

    private static RoomPlayer guest(String id, String name) {
        return RoomPlayer.of(PlayerId.of(id), name, false, false);
    }

    @Test
    void create_startsInLobbyWithHostInside() {
        Room room = Room.create(RoomCode.generate(), host());

        assertThat(room.phase()).isEqualTo(GamePhase.LOBBY);
        assertThat(room.players()).contains(host());
    }

    @Test
    void create_keepsTheGivenCode() {
        RoomCode code = RoomCode.generate();
        Room room = Room.create(code, host());

        assertThat(room.code()).isEqualTo(code);
    }

    @Test
    void create_rejectsNullCode() {
        assertThatThrownBy(() -> Room.create(null, host()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_rejectsNullHost() {
        assertThatThrownBy(() -> Room.create(RoomCode.generate(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_marksTheCreatorAsHost() {
        RoomPlayer notMarked = RoomPlayer.of(PlayerId.of("host-1"), "Alice", false, false);
        Room room = Room.create(RoomCode.generate(), notMarked);

        RoomPlayer stored = room.players().iterator().next();
        assertThat(stored.isHost()).isTrue();
    }

    @Test
    void join_addsPlayer() {
        Room room = Room.create(RoomCode.generate(), host());

        room.join(guest("p2", "Bob"));

        assertThat(room.players()).hasSize(2);
    }

    @Test
    void join_isIdempotentForSamePlayer() {
        Room room = Room.create(RoomCode.generate(), host());
        RoomPlayer bob = guest("p2", "Bob");

        room.join(bob);
        room.join(bob);

        assertThat(room.players()).hasSize(2);
    }

    @Test
    void leave_removesPlayer() {
        Room room = Room.create(RoomCode.generate(), host());
        RoomPlayer bob = guest("p2", "Bob");
        room.join(bob);

        room.leave(bob.id());

        assertThat(room.players()).doesNotContain(bob);
    }
}