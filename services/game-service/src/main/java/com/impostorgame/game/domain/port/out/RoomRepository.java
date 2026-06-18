package com.impostorgame.game.domain.port.out;

import com.impostorgame.game.domain.model.Room;
import com.impostorgame.game.domain.model.RoomCode;

public interface RoomRepository {
    Room save(Room room);
    boolean existsByCode(RoomCode code);
}