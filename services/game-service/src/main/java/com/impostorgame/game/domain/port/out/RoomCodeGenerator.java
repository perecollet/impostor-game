package com.impostorgame.game.domain.port.out;

import com.impostorgame.game.domain.model.RoomCode;

public interface RoomCodeGenerator {
    RoomCode generate();
}