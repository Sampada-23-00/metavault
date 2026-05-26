package com.sampada.metavault.controller;

import com.sampada.metavault.dto.AuthResponse;
import com.sampada.metavault.dto.LoginRequest;
import com.sampada.metavault.dto.RegisterRequest;
import com.sampada.metavault.dto.UserResponse;
import com.sampada.metavault.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController — handles all authentication endpoints.
 *
 * Controller responsibilities (and ONLY these):
 * 1. Receive the HTTP request
 * 2. Validate input (@Valid)
 * 3. Delegate to service
 * 4. Return HTTP response
 *
 * NO business logic here. No DB calls. Just routing + delegation.
 *
 * @Tag — Swagger grouping label (shows up in Swagger UI as a section)
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Register, login, and get current user info")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/register
     *
     * @Valid — triggers Jakarta Bean Validation on RegisterRequest fields.
     *          If @NotBlank or @Size fails, Spring throws MethodArgumentNotValidException
     *          before this method even runs. GlobalExceptionHandler returns 400.
     *
     * @RequestBody — deserializes the JSON request body into a RegisterRequest object.
     *
     * Returns 201 Created (not 200 OK) — semantically correct for resource creation.
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user",
               description = "Creates a new account and returns a JWT token")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login
     *
     * Returns 200 OK with the JWT token.
     * The client must store this token and send it on every subsequent request.
     */
    @PostMapping("/login")
    @Operation(summary = "Login",
               description = "Authenticate with username and password, returns a JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * GET /api/auth/me
     *
     * @AuthenticationPrincipal — Spring injects the currently authenticated user
     * (the UserDetails object stored in SecurityContext by JwtAuthFilter).
     * We just need the username from it.
     *
     * This endpoint requires a valid JWT (configured in SecurityConfig).
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user",
               description = "Returns the profile of the currently authenticated user")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.getCurrentUser(userDetails.getUsername()));
    }
}
