package com.impostorgame.game.infrastructure.adapter.out.persistence;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerRedisEntity {
    private String id;
    private String displayName;
    private boolean isHost;
    private boolean isGuest;
}