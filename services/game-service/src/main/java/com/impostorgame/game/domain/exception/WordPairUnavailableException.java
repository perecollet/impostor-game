package com.impostorgame.game.domain.exception;

public class WordPairUnavailableException extends RuntimeException {

    public WordPairUnavailableException(String message) {
        super(message);
    }
    public WordPairUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
