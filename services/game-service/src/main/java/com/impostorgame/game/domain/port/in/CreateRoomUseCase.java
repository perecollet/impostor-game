package com.impostorgame.game.domain.port.in;

public interface CreateRoomUseCase{

    RoomResponse createRoom(CreateRoomCommand command);

    record CreateRoomCommand(String playerId, String displayName, boolean isGuest){}
}