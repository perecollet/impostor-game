package com.impostorgame.game.infrastructure.adapter.in.web;

import com.impostorgame.game.domain.exception.InvalidPlayerIdException;
import com.impostorgame.game.domain.exception.InvalidRoomCodeException;
import com.impostorgame.game.domain.exception.InvalidRoomException;
import com.impostorgame.game.domain.exception.InvalidRoomPlayerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String DEFAULT_VALIDATION_MESSAGE = "Validation error.";

    @ExceptionHandler({
            InvalidRoomException.class,
            InvalidRoomPlayerException.class,
            InvalidRoomCodeException.class,
            InvalidPlayerIdException.class
    })
    public ProblemDetail handleDomainException(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse(DEFAULT_VALIDATION_MESSAGE);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
    }
}
