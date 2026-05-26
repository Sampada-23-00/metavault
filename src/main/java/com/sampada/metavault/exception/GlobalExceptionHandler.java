package com.sampada.metavault.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @RestControllerAdvice = global exception handler for all @RestController classes.
 * Instead of try-catch in every controller method, we handle exceptions here ONCE.
 *
 * How it works:
 * 1. A controller method throws an exception
 * 2. Spring looks for a matching @ExceptionHandler in this class
 * 3. That handler runs and returns the HTTP error response
 *
 * Every error response has the same structure:
 * {
 *   "timestamp": "2026-05-26T10:00:00",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "MetadataRecord not found with id: ...",
 *   "path": "/api/records/abc"
 * }
 * Consistent error format = easier for API consumers to handle errors.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── 404 Not Found ─────────────────────────────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    // ── 403 Forbidden ─────────────────────────────────────────────────────────
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(
            UnauthorizedException ex, HttpServletRequest request) {
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI());
    }

    // ── 401 Bad Credentials (wrong password at login) ─────────────────────────
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        return buildError(HttpStatus.UNAUTHORIZED, "Invalid username or password", request.getRequestURI());
    }

    // ── 400 Validation errors (@Valid on DTOs) ────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        // Collect all field errors: {"username": "must not be blank", "email": "invalid format"}
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Failed");
        body.put("message", "Input validation failed");
        body.put("fieldErrors", fieldErrors);
        body.put("path", request.getRequestURI());

        return ResponseEntity.badRequest().body(body);
    }

    // ── 400 Illegal argument (e.g., username already taken) ───────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    // ── 500 Catch-all for unexpected errors ────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(
            Exception ex, HttpServletRequest request) {
        // Log the full stack trace at ERROR level — useful for debugging
        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI());
    }

    // ── Helper: builds the standard error response body ───────────────────────
    private ResponseEntity<Map<String, Object>> buildError(
            HttpStatus status, String message, String path) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        return ResponseEntity.status(status).body(body);
    }
}
