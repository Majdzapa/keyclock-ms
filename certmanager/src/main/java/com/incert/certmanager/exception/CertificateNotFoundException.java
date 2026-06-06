package com.incert.certmanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CertificateNotFoundException extends RuntimeException {
    public CertificateNotFoundException(Long id) {
        super("Certificate not found with ID: " + id);
    }
    public CertificateNotFoundException(String message) {
        super(message);
    }
}
