package com.impostorgame.game.application.service;

import com.impostorgame.game.domain.exception.RoomCodeGenerationException;
import com.impostorgame.game.domain.model.Room;
import com.impostorgame.game.domain.port.in.CreateRoomUseCase;
import com.impostorgame.game.domain.port.in.PlayerResponse;
import com.impostorgame.game.domain.port.in.RoomResponse;
import com.impostorgame.game.domain.port.out.RoomCodeGenerator;
import com.impostorgame.game.domain.port.out.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CreateRoomService implements CreateRoomUseCase {

    static final int MAX_ATTEMPTS = 5;

    private final RoomRepository roomRepository;
    private final RoomCodeGenerator roomCodeGenerator;

    public CreateRoomService(RoomRepository roomRepository,  RoomCodeGenerator roomCodeGenerator) {
        this.roomRepository = roomRepository;
        this.roomCodeGenerator = roomCodeGenerator;
    }

    public RoomResponse createRoom(CreateRoomCommand command) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Room room = Room.create(roomCodeGenerator.generate(), command.player());
            Optional<Room> saved = roomRepository.saveIfAbsent(room);
            if (saved.isPresent()) {
                return toResponse(saved.get());
            }
        }

        throw new RoomCodeGenerationException("Could not generate a unique room code after " + MAX_ATTEMPTS + " attempts");
    }

    private RoomResponse toResponse(Room room) {
        List<PlayerResponse> players = room.players().values().stream()
                .map(p  -> new PlayerResponse(p.id().value(), p.displayName(), p.isHost(), p.isGuest()))
                .toList();
        return new RoomResponse(room.code().value(), room.phase().name(), players);
    }

}
