package com.impostorgame.game.infrastructure.adapter.out;

import com.impostorgame.game.domain.model.RoomCode;
import com.impostorgame.game.domain.port.out.RoomCodeGenerator;
import org.springframework.stereotype.Component;

@Component
public class RoomCodeGeneratorAdapter implements RoomCodeGenerator {

    @Override
    public RoomCode generate() {
        return RoomCode.generate();
    }
}
