package com.incert.certmanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidCertificateFormatException extends RuntimeException {
    public InvalidCertificateFormatException(String message) {
        super(message);
    }
}
