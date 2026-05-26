package com.sampada.metavault.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * What the client sends for login.
 * Deliberately minimal — just username + password.
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
