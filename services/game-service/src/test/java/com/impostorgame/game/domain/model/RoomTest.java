package com.impostorgame.game.domain.model;

import com.impostorgame.game.domain.exception.InvalidRoomException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

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
        assertThat(room.players()).containsKey(host().id());
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
                .isInstanceOf(InvalidRoomException.class);
    }

    @Test
    void create_rejectsNullHost() {
        assertThatThrownBy(() -> Room.create(RoomCode.generate(), null))
                .isInstanceOf(InvalidRoomException.class);
    }

    @Test
    void create_marksTheCreatorAsHost() {
        RoomPlayer notMarked = RoomPlayer.of(PlayerId.of("host-1"), "Alice", false, false);
        Room room = Room.create(RoomCode.generate(), notMarked);

        RoomPlayer stored = room.players().get(notMarked.id());
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
    void join_updatesExistingPlayerInsteadOfIgnoringTheRejoin() {
        Room room = Room.create(RoomCode.generate(), host());
        RoomPlayer bob = guest("p2", "Bob");
        room.join(bob);

        RoomPlayer promotedBob = RoomPlayer.of(bob.id(), "Bobby", true, false);
        room.join(promotedBob);

        RoomPlayer stored = room.players().get(bob.id());
        assertThat(room.players()).hasSize(2);
        assertThat(stored.displayName()).isEqualTo("Bobby");
        assertThat(stored.isHost()).isTrue();
    }

    @Test
    void leave_removesPlayer() {
        Room room = Room.create(RoomCode.generate(), host());
        RoomPlayer bob = guest("p2", "Bob");
        room.join(bob);

        room.leave(bob.id());

        assertThat(room.players()).doesNotContainKey(bob.id());
    }

    @Test
    void restore_rebuildsRoomWithGivenPhaseAndPlayers() {
        RoomCode code = RoomCode.generate();
        RoomPlayer alice = RoomPlayer.of(PlayerId.of("host-1"), "Alice", true, false);
        RoomPlayer bob = RoomPlayer.of(PlayerId.of("p2"), "Bob", false, false);
        Map<PlayerId, RoomPlayer> players = Map.of(alice.id(), alice, bob.id(), bob);

        Room room = Room.restore(code, GamePhase.DISCUSSION, players);

        assertThat(room.code()).isEqualTo(code);
        assertThat(room.phase()).isEqualTo(GamePhase.DISCUSSION);
        assertThat(room.players()).hasSize(2);
    }

    @Test
    void restore_rejectsNullCode() {
        RoomPlayer alice = RoomPlayer.of(PlayerId.of("host-1"), "Alice", true, false);
        Map<PlayerId, RoomPlayer> players = Map.of(alice.id(), alice);
        assertThatThrownBy(() -> Room.restore(null, GamePhase.LOBBY, players))
                .isInstanceOf(InvalidRoomException.class);
    }

    @Test
    void restore_rejectsNullPhase() {
        RoomPlayer alice = RoomPlayer.of(PlayerId.of("host-1"), "Alice", true, false);
        Map<PlayerId, RoomPlayer> players = Map.of(alice.id(), alice);
        assertThatThrownBy(() -> Room.restore(RoomCode.generate(), null, players))
                .isInstanceOf(InvalidRoomException.class);
    }

    @Test
    void restore_rejectsEmptyPlayers() {
        assertThatThrownBy(() -> Room.restore(RoomCode.generate(), GamePhase.LOBBY, Map.of()))
                .isInstanceOf(InvalidRoomException.class);
    }

    @Test
    void restore_rejectsNullPlayers() {
        assertThatThrownBy(() -> Room.restore(RoomCode.generate(), GamePhase.LOBBY, null))
                .isInstanceOf(InvalidRoomException.class);
    }

    @Test
    void join_rejectsWhenPhaseIsNotLobby() {
        RoomPlayer alice = RoomPlayer.of(PlayerId.of("host-1"), "Alice", true, false);
        Map<PlayerId, RoomPlayer> players = Map.of(alice.id(), alice);
        Room room = Room.restore(RoomCode.generate(), GamePhase.DISCUSSION, players);
        RoomPlayer latecomer = guest("p2", "Bob");

        assertThatThrownBy(() -> room.join(latecomer))
                .isInstanceOf(InvalidRoomException.class);
    }

    @Test
    void join_rejectsNullPlayer() {
        Room room = Room.create(RoomCode.generate(), host());

        assertThatThrownBy(() -> room.join(null))
                .isInstanceOf(InvalidRoomException.class);
    }
}