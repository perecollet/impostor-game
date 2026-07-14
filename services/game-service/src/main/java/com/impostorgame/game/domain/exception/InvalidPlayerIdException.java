package com.impostorgame.game.domain.exception;

public class InvalidPlayerIdException extends RuntimeException {

    public InvalidPlayerIdException(String message) {
        super(message);
    }
}
