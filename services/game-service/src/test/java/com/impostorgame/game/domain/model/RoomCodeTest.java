package com.impostorgame.game.domain.model;

import com.impostorgame.game.domain.exception.InvalidRoomCodeException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
public class RoomCodeTest {

    @Test
    void generate_shouldReturnSixCharacterCode(){
        RoomCode code = RoomCode.generate();
        assertThat(code.value()).hasSize(6);
    }

    @Test
    void generate_shouldReturnOnlyUnambiguousCharacters() {
        for (int i = 0; i < 500; i++) {
            RoomCode code = RoomCode.generate();
            assertThat(code.value()).matches("[A-HJ-NP-Z2-9]{6}");
        }
    }

    @Test
    void constructor_shouldThrowWhenContainsInvalidCharacters() {
        assertThatThrownBy(() -> new RoomCode("ABC1EF"))
                .isInstanceOf(InvalidRoomCodeException.class);
        assertThatThrownBy(() -> new RoomCode("abcdef"))
                .isInstanceOf(InvalidRoomCodeException.class);
        assertThatThrownBy(() -> new RoomCode("ABC!EF"))
                .isInstanceOf(InvalidRoomCodeException.class);
    }

    @Test
    void constructor_shouldThrowWhenBlank() {
        assertThatThrownBy(() -> new RoomCode(""))
                .isInstanceOf(InvalidRoomCodeException.class);
    }

    @Test
    void constructor_shouldThrowWhenNotSixCharacters() {
        assertThatThrownBy(() -> new RoomCode("AB"))
                .isInstanceOf(InvalidRoomCodeException.class);
    }

    @Test
    void constructor_shouldThrowWhenNull() {
        assertThatThrownBy(() -> new RoomCode(null))
                .isInstanceOf(InvalidRoomCodeException.class);
    }

    @Test
    void twoGeneratedCodes_shouldBeDifferent() {
        RoomCode code1 = RoomCode.generate();
        RoomCode code2 = RoomCode.generate();
        assertThat(code1).isNotEqualTo(code2);
    }
}
