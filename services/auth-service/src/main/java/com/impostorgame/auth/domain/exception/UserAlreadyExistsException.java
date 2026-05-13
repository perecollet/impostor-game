package com.impostorgame.auth.domain.exception;

public class UserAlreadyExistsException extends RuntimeException {

    private static final String MESSAGE = "User already exists with email: %s";

    public UserAlreadyExistsException(String email) {
        super(String.format(MESSAGE, email));
    }
}
