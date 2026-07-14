package com.impostorgame.game.domain.port.out;

import com.impostorgame.game.domain.model.Room;

import java.util.Optional;

public interface RoomRepository {
    Optional<Room> saveIfAbsent(Room room);
}
