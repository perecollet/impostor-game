package com.impostorgame.game.infrastructure.adapter.out.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomRedisEntity {
    private String code;
    private String phase;
    private Map<String, PlayerRedisEntity> players;

}