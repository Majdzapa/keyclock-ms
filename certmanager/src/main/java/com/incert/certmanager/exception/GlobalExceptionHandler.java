package com.incert.certmanager.exception;

import com.incert.certmanager.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CertificateNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(
            CertificateNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler({CertificateParsingException.class, InvalidCertificateFormatException.class})
    public ResponseEntity<ErrorResponseDto> handleBadRequest(
            RuntimeException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(UnauthorizedGroupAccessException.class)
    public ResponseEntity<ErrorResponseDto> handleGroupAccess(
            UnauthorizedGroupAccessException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "Forbidden",
            "You do not have permission to perform this action", request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponseDto.FieldError> fieldErrors = ex.getBindingResult()
            .getAllErrors()
            .stream()
            .filter(FieldError.class::isInstance)
            .map(FieldError.class::cast)
            .map(fe -> new ErrorResponseDto.FieldError(fe.getField(), fe.getDefaultMessage()))
            .toList();

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Failed",
            "Request validation failed — check field errors", request, fieldErrors);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDto> handleMaxUploadSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("File upload size exceeded: {}", ex.getMessage());
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "Payload Too Large",
            "Uploaded file exceeds the maximum allowed size (10 MB)", request, List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
            "An unexpected error occurred. Please try again later.", request, List.of());
    }

    // ─────────────────────────── Builder ──────────────────────────────────

    private ResponseEntity<ErrorResponseDto> buildResponse(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request,
            List<ErrorResponseDto.FieldError> fieldErrors) {

        ErrorResponseDto body = new ErrorResponseDto(
            LocalDateTime.now(),
            status.value(),
            error,
            message,
            request.getRequestURI(),
            fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}
