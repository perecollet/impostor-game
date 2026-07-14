package com.impostorgame.game.domain.model;

import com.impostorgame.game.domain.exception.InvalidRoomException;

import java.util.HashSet;
import java.util.Set;

public class Room {

    private final RoomCode roomCode;
    private GamePhase gamePhase;
    private final Set<RoomPlayer> roomPlayers;

    private Room (RoomCode roomCode, RoomPlayer host) {

        if (roomCode == null) throw new InvalidRoomException("RoomCode must not be null");
        if (host == null) throw new InvalidRoomException("RoomPlayer must not be null");

        this.roomCode = roomCode;
        this.gamePhase = GamePhase.LOBBY;
        this.roomPlayers = new HashSet<>();
        roomPlayers.add(RoomPlayer.of(host.id(), host.displayName(), true, host.isGuest()));
    }

    private Room(RoomCode roomCode, GamePhase gamePhase, Set<RoomPlayer> roomPlayers){
        if (roomCode == null) throw new InvalidRoomException("RoomCode must not be null");
        if (gamePhase == null) throw new InvalidRoomException("GamePhase must not be null");
        if (roomPlayers == null || roomPlayers.isEmpty()) throw new InvalidRoomException("roomPlayers must not be null or empty");

        this.roomCode = roomCode;
        this.gamePhase = gamePhase;
        this.roomPlayers = new HashSet<>(roomPlayers);
    }

    public static Room create(RoomCode roomCode, RoomPlayer host) {
        return new Room(roomCode, host);
    }

    public GamePhase phase(){
        return this.gamePhase;
    }

    public Set<RoomPlayer> players(){
        return Set.copyOf(this.roomPlayers);
    }

    public RoomCode code(){
        return this.roomCode;
    }

    public void join(RoomPlayer roomPlayer) {
        if (roomPlayer == null) throw new InvalidRoomException("RoomPlayer must not be null");
        this.roomPlayers.add(roomPlayer);
    }

    public void leave(PlayerId id) {
        if (id == null) throw new InvalidRoomException("Id must not be null");
        this.roomPlayers.removeIf(p -> p.id().equals(id));
    }

    public static Room restore(RoomCode roomCode, GamePhase gamePhase, Set<RoomPlayer> roomPlayers) {
        return new Room(roomCode, gamePhase, roomPlayers);
    }
}
