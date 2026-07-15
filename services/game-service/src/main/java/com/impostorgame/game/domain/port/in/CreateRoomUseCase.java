package com.impostorgame.game.domain.port.in;

import com.impostorgame.game.domain.model.PlayerContext;

public interface CreateRoomUseCase{

    RoomResponse createRoom(CreateRoomCommand command);

    record CreateRoomCommand(PlayerContext player){}
}