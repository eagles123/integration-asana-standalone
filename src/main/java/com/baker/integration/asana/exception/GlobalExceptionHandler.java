package com.baker.integration.asana.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AsanaSignatureException.class)
    public ResponseEntity<Map<String, String>> handleSignatureException(AsanaSignatureException e) {
        log.warn("Asana signature verification failed: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid request signature"));
    }

    @ExceptionHandler(DamAuthenticationRequiredException.class)
    public ResponseEntity<Map<String, String>> handleDamAuthRequired(DamAuthenticationRequiredException e) {
        log.warn("DAM authentication required: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "User not authenticated with Lytho DAM"));
    }

    @ExceptionHandler(AsanaApiException.class)
    public ResponseEntity<Map<String, String>> handleAsanaApiException(AsanaApiException e) {
        log.error("Asana API error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "Failed to communicate with Asana API"));
    }

    @ExceptionHandler(FileTransferException.class)
    public ResponseEntity<Map<String, String>> handleFileTransferException(FileTransferException e) {
        log.error("File transfer error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "File transfer failed"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Invalid integration request: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
    }
}
