package com.incert.certmanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CertificateParsingException extends RuntimeException {
    public CertificateParsingException(String message) {
        super(message);
    }
    public CertificateParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
