package com.impostorgame.game.application.service;

import com.impostorgame.game.domain.model.PlayerId;
import com.impostorgame.game.domain.model.Room;
import com.impostorgame.game.domain.model.RoomCode;
import com.impostorgame.game.domain.model.RoomPlayer;
import com.impostorgame.game.domain.port.in.CreateRoomUseCase;
import com.impostorgame.game.domain.port.in.PlayerResponse;
import com.impostorgame.game.domain.port.in.RoomResponse;
import com.impostorgame.game.domain.port.out.RoomCodeGenerator;
import com.impostorgame.game.domain.port.out.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CreateRoomService implements CreateRoomUseCase {

    private final RoomRepository roomRepository;
    private final RoomCodeGenerator roomCodeGenerator;

    public CreateRoomService(RoomRepository roomRepository,  RoomCodeGenerator roomCodeGenerator) {
        this.roomRepository = roomRepository;
        this.roomCodeGenerator = roomCodeGenerator;
    }

    public RoomResponse createRoom(CreateRoomCommand command) {
        RoomPlayer host = RoomPlayer.of(PlayerId.of(command.playerId()), command.displayName(), false, command.isGuest());
        Room room = roomRepository.save(Room.create(generateRoomCode(), host));
        return toResponse(room);
    }

    private RoomResponse toResponse(Room room) {
        List<PlayerResponse> players = room.players().values().stream()
                .map(p  -> new PlayerResponse(p.id().value(), p.displayName(), p.isHost(), p.isGuest()))
                .toList();
        return new RoomResponse(room.code().value(), room.phase().name(), players);
    }

    private RoomCode generateRoomCode() {
        RoomCode code = roomCodeGenerator.generate();
        while(roomRepository.existsByCode(code)) code = roomCodeGenerator.generate();
        return code;
    }

}
