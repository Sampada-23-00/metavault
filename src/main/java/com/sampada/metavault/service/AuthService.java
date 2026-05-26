package com.sampada.metavault.service;

import com.sampada.metavault.dto.AuthResponse;
import com.sampada.metavault.dto.LoginRequest;
import com.sampada.metavault.dto.RegisterRequest;
import com.sampada.metavault.dto.UserResponse;
import com.sampada.metavault.entity.User;
import com.sampada.metavault.enums.Role;
import com.sampada.metavault.repository.UserRepository;
import com.sampada.metavault.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthService contains all the business logic for authentication.
 * Controllers are kept thin — they delegate everything here.
 *
 * @Transactional on the class: every public method runs inside a DB transaction.
 * If anything throws an exception, the transaction rolls back automatically.
 */
@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // Constructor injection — all dependencies are final (immutable after construction)
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Register a new user.
     *
     * Steps:
     * 1. Check username/email uniqueness (fail fast with clear error messages)
     * 2. Hash the password with BCrypt
     * 3. Save the user to DB
     * 4. Generate a JWT so they're immediately logged in (no separate login step needed)
     * 5. Return token + user info
     */
    public AuthResponse register(RegisterRequest request) {
        // Uniqueness checks — better UX than letting DB constraints throw generic errors
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email '" + request.getEmail() + "' is already registered");
        }

        // Build the User entity using Lombok's builder pattern
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                // encode() runs BCrypt — the raw password is never stored anywhere
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getUsername());

        // Generate JWT immediately — user is registered and logged in
        String token = jwtService.generateToken(savedUser);

        return AuthResponse.builder()
                .token(token)
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }

    /**
     * Log in an existing user.
     *
     * Steps:
     * 1. authenticationManager.authenticate() does the heavy lifting:
     *    - Calls CustomUserDetailsService.loadUserByUsername() to fetch the user
     *    - Calls passwordEncoder.matches() to verify the password
     *    - Throws BadCredentialsException if either check fails
     * 2. If authenticate() returns without throwing, credentials are valid
     * 3. Load the user entity (to get all fields for the response)
     * 4. Generate JWT and return
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // This single line does: load user from DB + verify password hash
        // If wrong credentials → throws BadCredentialsException → GlobalExceptionHandler → 401
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        log.info("User logged in: {}", user.getUsername());

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    /**
     * Get the current user's profile.
     * The username comes from the JWT (extracted by JwtAuthFilter,
     * stored in SecurityContext, passed in by AuthController).
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
