package com.impostorgame.game.infrastructure.adapter.out.persistence;

import com.impostorgame.game.domain.model.GamePhase;
import com.impostorgame.game.domain.model.PlayerId;
import com.impostorgame.game.domain.model.Room;
import com.impostorgame.game.domain.model.RoomCode;
import com.impostorgame.game.domain.model.RoomPlayer;
import com.impostorgame.game.domain.port.out.RoomRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class RoomRepositoryAdapter implements RoomRepository {

    private static final String KEY_PREFIX = "room:";
    private static final Duration TTL = Duration.ofSeconds(3600);

    private final RedisTemplate<String, RoomRedisEntity> redisTemplate;

    public RoomRepositoryAdapter(RedisTemplate<String, RoomRedisEntity> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<Room> saveIfAbsent(Room room) {
        RoomRedisEntity entity = toEntity(room);
        Boolean saved = redisTemplate.opsForValue().setIfAbsent(key(room.code()), entity, TTL);
        return Boolean.TRUE.equals(saved) ? Optional.of(toDomain(entity)) : Optional.empty();
    }

    private String key(RoomCode code) {
        return KEY_PREFIX + code.value();
    }

    private RoomRedisEntity toEntity(Room room) {
        Map<String, PlayerRedisEntity> players = room.players().values().stream()
                .map(p -> new PlayerRedisEntity(p.id().value(), p.displayName(), p.isHost(), p.isGuest()))
                .collect(Collectors.toMap(PlayerRedisEntity::getId, Function.identity()));

        return RoomRedisEntity.builder()
                .code(room.code().value())
                .phase(room.phase().name())
                .players(players)
                .build();

    }

    private Room toDomain(RoomRedisEntity entity) {
        RoomCode code = RoomCode.of(entity.getCode());
        Map<PlayerId, RoomPlayer> players = entity.getPlayers().values().stream()
                .map(p -> RoomPlayer.restore(PlayerId.of(p.getId()), p.getDisplayName(), p.isHost(), p.isGuest()))
                .collect(Collectors.toMap(RoomPlayer::id, Function.identity()));
        return Room.restore(code, GamePhase.valueOf(entity.getPhase()), players);
    }
}
