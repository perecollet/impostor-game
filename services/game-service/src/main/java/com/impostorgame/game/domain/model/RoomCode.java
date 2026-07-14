package com.impostorgame.game.domain.model;

import com.impostorgame.game.domain.exception.InvalidRoomCodeException;

import java.security.SecureRandom;
import java.util.regex.Pattern;

public record RoomCode(String value) {

    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 6;
    private static final Pattern PATTERN = Pattern.compile("[A-HJ-NP-Z2-9]{6}");
    private static final SecureRandom RANDOM = new SecureRandom();

    public RoomCode {
        if (value == null || value.isBlank()) {
            throw new InvalidRoomCodeException("Room code value cannot be blank");
        }

        if (value.length() != LENGTH) {
            throw new InvalidRoomCodeException("Room code must be exactly 6 characters");
        }

        if (!PATTERN.matcher(value).matches()) {
            throw new InvalidRoomCodeException("Room code must be 6 characters and not contain invalid characters");
        }
    }

    public static RoomCode generate() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < LENGTH; i++) {
            code.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return new RoomCode(code.toString());
    }

    public static RoomCode of(String value){
        return new RoomCode(value);
    }

}