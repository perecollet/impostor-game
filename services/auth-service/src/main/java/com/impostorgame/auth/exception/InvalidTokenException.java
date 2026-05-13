package com.impostorgame.auth.exception;

public class InvalidTokenException extends RuntimeException {

    private static final String MESSAGE = "Invalid token: %s";

    public InvalidTokenException(String message) {
        super(String.format(MESSAGE, message));
    }
}
