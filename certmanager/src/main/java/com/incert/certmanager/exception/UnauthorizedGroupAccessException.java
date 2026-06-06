package com.incert.certmanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedGroupAccessException extends RuntimeException {
    public UnauthorizedGroupAccessException(String message) {
        super(message);
    }
}
