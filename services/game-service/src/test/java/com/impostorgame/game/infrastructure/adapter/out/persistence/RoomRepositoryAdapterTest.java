package com.impostorgame.game.infrastructure.adapter.out.persistence;

import com.impostorgame.game.domain.model.PlayerId;
import com.impostorgame.game.domain.model.RegisteredPlayer;
import com.impostorgame.game.domain.model.Room;
import com.impostorgame.game.domain.model.RoomCode;
import com.impostorgame.game.domain.model.RoomPlayer;
import com.impostorgame.game.domain.port.out.RoomRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
class RoomRepositoryAdapterTest {

    @Container
    @ServiceConnection("redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379);

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RedisTemplate<String, RoomRedisEntity> redisTemplate;

    @Test
    void saveIfAbsent_persistsRoomAndCanBeRetrievedFromRedis() {
        Room room = Room.create(RoomCode.generate(), new RegisteredPlayer("host-1", "Alice"));

        Optional<Room> saved = roomRepository.saveIfAbsent(room);

        assertThat(saved).isPresent();
        assertThat(saved.get().code()).isEqualTo(room.code());
        assertThat(saved.get().players()).containsKey(PlayerId.of("host-1"));
        assertThat(saved.get().players().get(PlayerId.of("host-1")).displayName()).isEqualTo("Alice");
    }

    @Test
    void saveIfAbsent_setsExpirationOnTheRedisKey() {
        Room room = Room.create(RoomCode.generate(), new RegisteredPlayer("host-2", "Bob"));

        roomRepository.saveIfAbsent(room);

        Long ttl = redisTemplate.getExpire("room:" + room.code().value());

        assertThat(ttl).isNotNull();
        assertThat(ttl).isGreaterThan(0);
    }
}
