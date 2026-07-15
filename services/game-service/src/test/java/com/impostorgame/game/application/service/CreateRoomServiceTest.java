package com.impostorgame.game.application.service;

import com.impostorgame.game.domain.exception.RoomCodeGenerationException;
import com.impostorgame.game.domain.model.GuestPlayer;
import com.impostorgame.game.domain.model.RegisteredPlayer;
import com.impostorgame.game.domain.model.Room;
import com.impostorgame.game.domain.model.RoomCode;
import com.impostorgame.game.domain.port.in.CreateRoomUseCase.CreateRoomCommand;
import com.impostorgame.game.domain.port.in.PlayerResponse;
import com.impostorgame.game.domain.port.in.RoomResponse;
import com.impostorgame.game.domain.port.out.RoomCodeGenerator;
import com.impostorgame.game.domain.port.out.RoomRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CreateRoomServiceTest {

    @Mock private RoomRepository roomRepository;
    @Mock private RoomCodeGenerator roomCodeGenerator;

    @InjectMocks private CreateRoomService createRoomService;

    @Test
    void createRoom_returnsResponseWithGeneratedCodeAndHost() {
        when(roomCodeGenerator.generate()).thenReturn(new RoomCode("ABCDEF"));
        when(roomRepository.saveIfAbsent(any(Room.class))).thenAnswer(inv -> Optional.of(inv.getArgument(0)));

        RoomResponse response = createRoomService.createRoom(
                new CreateRoomCommand(new RegisteredPlayer("user-1", "Alice")));

        assertThat(response.roomCode()).isEqualTo("ABCDEF");
        assertThat(response.phase()).isEqualTo("LOBBY");
        assertThat(response.players()).hasSize(1);
    }

    @Test
    void createRoom_marksCreatorAsHost() {
        when(roomCodeGenerator.generate()).thenReturn(new RoomCode("ABCDEF"));
        when(roomRepository.saveIfAbsent(any(Room.class))).thenAnswer(inv -> Optional.of(inv.getArgument(0)));

        RoomResponse response = createRoomService.createRoom(
                new CreateRoomCommand(new RegisteredPlayer("user-1", "Alice")));

        PlayerResponse creator = response.players().get(0);
        assertThat(creator.isHost()).isTrue();
        assertThat(creator.id()).isEqualTo("user-1");
        assertThat(creator.displayName()).isEqualTo("Alice");
        assertThat(creator.isGuest()).isFalse();
    }

    @Test
    void createRoom_preservesGuestFlag() {
        when(roomCodeGenerator.generate()).thenReturn(new RoomCode("ABCDEF"));
        when(roomRepository.saveIfAbsent(any(Room.class))).thenAnswer(inv -> Optional.of(inv.getArgument(0)));

        RoomResponse response = createRoomService.createRoom(
                new CreateRoomCommand(new GuestPlayer("guest-1", "RandomName")));

        assertThat(response.players().get(0).isGuest()).isTrue();
    }

    @Test
    void createRoom_retriesWithNewCode_whenCodeAlreadyTaken() {
        when(roomCodeGenerator.generate())
                .thenReturn(new RoomCode("AAAAAA"))
                .thenReturn(new RoomCode("BBBBBB"));
        when(roomRepository.saveIfAbsent(any(Room.class)))
                .thenReturn(Optional.empty())
                .thenAnswer(inv -> Optional.of(inv.getArgument(0)));

        RoomResponse response = createRoomService.createRoom(
                new CreateRoomCommand(new RegisteredPlayer("user-1", "Alice")));

        assertThat(response.roomCode()).isEqualTo("BBBBBB");
        verify(roomCodeGenerator, times(2)).generate();
        verify(roomRepository, times(2)).saveIfAbsent(any(Room.class));
    }

    @Test
    void createRoom_throwsRoomCodeGenerationException_whenRetriesExhausted() {
        when(roomCodeGenerator.generate()).thenReturn(new RoomCode("AAAAAA"));
        when(roomRepository.saveIfAbsent(any(Room.class))).thenReturn(Optional.empty());
        CreateRoomCommand command = new CreateRoomCommand(new RegisteredPlayer("user-1", "Alice"));

        assertThatThrownBy(() -> createRoomService.createRoom(command))
                .isInstanceOf(RoomCodeGenerationException.class);

        verify(roomRepository, times(CreateRoomService.MAX_ATTEMPTS)).saveIfAbsent(any(Room.class));
    }

    @Test
    void createRoom_persistsTheRoom() {
        when(roomCodeGenerator.generate()).thenReturn(new RoomCode("ABCDEF"));
        when(roomRepository.saveIfAbsent(any(Room.class))).thenAnswer(inv -> Optional.of(inv.getArgument(0)));

        createRoomService.createRoom(new CreateRoomCommand(new RegisteredPlayer("user-1", "Alice")));

        verify(roomRepository).saveIfAbsent(any(Room.class));
    }
}