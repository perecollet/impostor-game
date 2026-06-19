package com.impostorgame.game.infrastructure.adapter.out.persistence;

import com.impostorgame.game.domain.model.GamePhase;
import com.impostorgame.game.domain.model.PlayerId;
import com.impostorgame.game.domain.model.Room;
import com.impostorgame.game.domain.model.RoomCode;
import com.impostorgame.game.domain.model.RoomPlayer;
import com.impostorgame.game.domain.port.out.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RoomRepositoryAdapter implements RoomRepository {

    private final RoomRedisRepository redisRepository;

    @Override
    public Room save(Room room) {
        RoomRedisEntity saved = redisRepository.save(toEntity(room));
        return toDomain(saved);
    }

    @Override
    public boolean existsByCode(RoomCode code) {
        return redisRepository.existsById(code.value());
    }

    private RoomRedisEntity toEntity(Room room) {
        List<PlayerRedisEntity> players = room.players().stream()
                .map(p -> new PlayerRedisEntity(p.id().value(), p.displayName(), p.isHost(), p.isGuest()))
                .toList();

        return RoomRedisEntity.builder()
                .code(room.code().value())
                .phase(room.phase().name())
                .players(players)
                .build();

    }

    private Room toDomain(RoomRedisEntity entity) {
        RoomCode code = RoomCode.of(entity.getCode());
        Set<RoomPlayer> players = entity.getPlayers().stream()
                .map(p -> RoomPlayer.of(PlayerId.of(p.getId()), p.getDisplayName(), p.isHost(), p.isGuest()))
                .collect(Collectors.toSet());
        return Room.restore(code, GamePhase.valueOf(entity.getPhase()),  players);
    }
}