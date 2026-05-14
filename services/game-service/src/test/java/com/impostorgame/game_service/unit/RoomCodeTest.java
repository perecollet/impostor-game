package com.impostorgame.game_service.unit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@Tag("unit")
public class RoomCodeTest {

    @Test
    void generate_shouldReturnSixCharacterCode(){
        RoomCode code = RoomCode.generate;
        assertThat(code.value()).hasSize(6);
    }

    @Test
    void generate_shouldReturnOnlyUppercaseLettersAndDigits() {
        RoomCode code = RoomCode.generate();
        assertThat(code.value()).matches("[A-Z0-9]{6}");
    }

    @Test
    void constructor_shouldThrowWhenBlank() {
        assertThatThrownBy(() -> new RoomCode(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_shouldThrowWhenNotSixCharacters() {
        assertThatThrownBy(() -> new RoomCode("AB"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_shouldThrowWhenNull() {
        assertThatThrownBy(() -> new RoomCode(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void twoGeneratedCodes_shouldBeDifferent() {
        RoomCode code1 = RoomCode.generate();
        RoomCode code2 = RoomCode.generate();
        assertThat(code1).isNotEqualTo(code2);
    }
}
