package com.impostorgame.game.infrastructure.adapter.out.persistence;

import org.springframework.data.repository.CrudRepository;

public interface RoomRedisRepository extends CrudRepository<RoomRedisEntity, String> {
}