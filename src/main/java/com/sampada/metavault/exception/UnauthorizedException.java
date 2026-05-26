package com.sampada.metavault.exception;

/**
 * Thrown when a user tries to access a resource they don't own.
 * The GlobalExceptionHandler catches this and returns HTTP 403 Forbidden.
 *
 * 401 Unauthorized = "I don't know who you are" (no/bad token)
 * 403 Forbidden    = "I know who you are, but you can't do this" (wrong owner)
 * This exception is for 403 scenarios.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public static UnauthorizedException accessDenied() {
        return new UnauthorizedException("You do not have permission to access this resource");
    }
}
