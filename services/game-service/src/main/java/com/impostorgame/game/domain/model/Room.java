package com.impostorgame.game.domain.model;

import com.impostorgame.game.domain.exception.InvalidRoomException;

import java.util.HashMap;
import java.util.Map;

public class Room {

    private final RoomCode roomCode;
    private GamePhase gamePhase;
    private final Map<PlayerId, RoomPlayer> roomPlayers;

    private Room (RoomCode roomCode, RoomPlayer host) {

        if (roomCode == null) throw new InvalidRoomException("RoomCode must not be null");
        if (host == null) throw new InvalidRoomException("RoomPlayer must not be null");

        this.roomCode = roomCode;
        this.gamePhase = GamePhase.LOBBY;
        this.roomPlayers = new HashMap<>();
        RoomPlayer hostPlayer = RoomPlayer.of(host.id(), host.displayName(), true, host.isGuest());
        roomPlayers.put(hostPlayer.id(), hostPlayer);
    }

    private Room(RoomCode roomCode, GamePhase gamePhase, Map<PlayerId, RoomPlayer> roomPlayers){
        if (roomCode == null) throw new InvalidRoomException("RoomCode must not be null");
        if (gamePhase == null) throw new InvalidRoomException("GamePhase must not be null");
        if (roomPlayers == null || roomPlayers.isEmpty()) throw new InvalidRoomException("roomPlayers must not be null or empty");

        this.roomCode = roomCode;
        this.gamePhase = gamePhase;
        this.roomPlayers = new HashMap<>(roomPlayers);
    }

    public static Room create(RoomCode roomCode, RoomPlayer host) {
        return new Room(roomCode, host);
    }

    public GamePhase phase(){
        return this.gamePhase;
    }

    public Map<PlayerId, RoomPlayer> players(){
        return Map.copyOf(this.roomPlayers);
    }

    public RoomCode code(){
        return this.roomCode;
    }

    public void join(RoomPlayer roomPlayer) {
        if (roomPlayer == null) throw new InvalidRoomException("RoomPlayer must not be null");
        if (gamePhase != GamePhase.LOBBY) throw new InvalidRoomException("Unable to join room in this phase");
        this.roomPlayers.put(roomPlayer.id(), roomPlayer);
    }

    public void leave(PlayerId id) {
        if (id == null) throw new InvalidRoomException("Id must not be null");
        this.roomPlayers.remove(id);
    }

    public static Room restore(RoomCode roomCode, GamePhase gamePhase, Map<PlayerId, RoomPlayer> roomPlayers) {
        return new Room(roomCode, gamePhase, roomPlayers);
    }
}
