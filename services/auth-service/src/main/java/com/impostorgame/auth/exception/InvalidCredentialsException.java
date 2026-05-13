package com.impostorgame.auth.exception;

public class InvalidCredentialsException extends RuntimeException {

    private static final String MESSAGE = "Invalid username or password";

    public InvalidCredentialsException() {
        super(MESSAGE);
    }
}
