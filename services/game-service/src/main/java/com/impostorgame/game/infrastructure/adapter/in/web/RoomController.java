package com.impostorgame.game.infrastructure.adapter.in.web;

import com.impostorgame.game.domain.model.Role;
import com.impostorgame.game.domain.port.in.CreateRoomUseCase;
import com.impostorgame.game.domain.port.in.RoomResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private final CreateRoomUseCase createRoomUseCase;

    public RoomController(CreateRoomUseCase createRoomUseCase) {
        this.createRoomUseCase = createRoomUseCase;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> newRoom(@AuthenticationPrincipal Jwt jwt) {
        RoomResponse response = createRoomUseCase.createRoom(toCommand(jwt));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{roomCode}")
                .buildAndExpand(response.roomCode())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    private CreateRoomUseCase.CreateRoomCommand toCommand (Jwt jwt){
        String playerId = jwt.getSubject();
        String displayName = jwt.getClaimAsString("displayName");
        boolean isGuest = Role.GUEST.name().equals(jwt.getClaimAsString("role"));

        return new CreateRoomUseCase.CreateRoomCommand(playerId, displayName, isGuest);
    }
}
