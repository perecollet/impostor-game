package com.impostorgame.game.infrastructure.adapter.out.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.util.Map;

@RedisHash(value = "room", timeToLive = 3600)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomRedisEntity {
    @Id
    private String code;
    private String phase;
    private Map<String, PlayerRedisEntity> players;

}