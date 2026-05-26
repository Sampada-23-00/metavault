package com.sampada.metavault.config;

import com.sampada.metavault.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Full JWT Security Configuration — replaces the temporary Phase 1 config.
 *
 * @EnableMethodSecurity: allows us to use @PreAuthorize on controller/service methods.
 *   Example: @PreAuthorize("hasRole('ADMIN')") on an admin-only endpoint.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, UserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    /**
     * The main security configuration — defines what is public and what needs a JWT.
     *
     * Key decisions:
     * - CSRF disabled: REST APIs are stateless; CSRF only matters for browser form submissions
     * - STATELESS sessions: we don't store sessions on the server — each request must carry a JWT
     * - Public routes: auth endpoints, health check, Swagger, H2 console
     * - Everything else: requires a valid JWT
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — not needed for stateless JWT APIs
            .csrf(AbstractHttpConfigurer::disable)

            // Allow H2 console to render in iframes (it uses frames internally)
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

            // Define which routes are public vs. protected
            .authorizeHttpRequests(auth -> auth
                // PUBLIC routes — no JWT needed
                .requestMatchers(
                    "/api/auth/**",          // register, login
                    "/api/health",           // health check
                    "/swagger-ui/**",        // Swagger UI static files
                    "/swagger-ui.html",      // Swagger UI redirect
                    "/api-docs/**",          // OpenAPI JSON
                    "/v3/api-docs/**",       // OpenAPI JSON (alternate path)
                    "/h2-console/**"         // H2 browser console (dev only)
                ).permitAll()
                // EVERYTHING ELSE requires authentication
                .anyRequest().authenticated()
            )

            // STATELESS: Spring should NOT create or use HTTP sessions.
            // Each request is authenticated independently via the JWT.
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Tell Spring Security which AuthenticationProvider to use
            .authenticationProvider(authenticationProvider())

            // Insert our JWT filter BEFORE Spring's default login filter.
            // Order matters: JWT filter runs first, sets authentication in SecurityContext,
            // then the rest of the chain runs with the user already authenticated.
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * DaoAuthenticationProvider: Spring Security's standard DB-backed auth provider.
     * It uses:
     *   - userDetailsService: to load the user from DB by username
     *   - passwordEncoder: to verify the submitted password against the stored BCrypt hash
     *
     * This is used during login: AuthService calls authenticationManager.authenticate()
     * which delegates to this provider.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * AuthenticationManager: the entry point for authentication.
     * AuthService calls authenticationManager.authenticate(username, password)
     * Spring wires this up automatically from AuthenticationConfiguration.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * BCryptPasswordEncoder: hashes passwords before storing, verifies during login.
     * BCrypt is a slow hash (by design) — makes brute-force attacks expensive.
     * Strength 10 = 2^10 = 1024 rounds (default, good balance of security vs. speed).
     *
     * Usage:
     *   String hash = passwordEncoder.encode("mypassword");    // on registration
     *   boolean match = passwordEncoder.matches("mypassword", hash); // on login
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
