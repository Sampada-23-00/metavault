package com.sampada.metavault.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO = Data Transfer Object.
 * This is what the client sends in the request body for registration.
 * We NEVER use the User entity directly in the API — that would expose
 * internal fields like passwordHash, role, etc.
 *
 * @Valid on the controller method + these annotations = automatic validation.
 * If validation fails, Spring throws MethodArgumentNotValidException
 * which our GlobalExceptionHandler catches and returns a 400 with field errors.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;
}
