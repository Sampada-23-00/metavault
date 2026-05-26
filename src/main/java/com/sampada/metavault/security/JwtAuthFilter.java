package com.sampada.metavault.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthFilter runs on EVERY incoming HTTP request — before it reaches any controller.
 *
 * It does ONE job: if the request has a valid JWT, authenticate the user.
 *
 * extends OncePerRequestFilter: guarantees this filter runs exactly once
 * per request (not multiple times in a redirect chain).
 *
 * THE FLOW for a protected request:
 * 1. Request arrives → filter runs
 * 2. Look for "Authorization: Bearer <token>" header
 * 3. If found → extract username from token
 * 4. Load user from DB (to get their roles/permissions)
 * 5. Validate token (username matches + not expired)
 * 6. If valid → tell Spring Security "this user is authenticated"
 * 7. Request continues to the controller
 *
 * THE FLOW for a public request (/api/auth/login):
 * 1. Request arrives → filter runs
 * 2. No Authorization header → skip authentication
 * 3. Request continues (SecurityConfig allows it via permitAll)
 *
 * @Component: Spring creates this as a bean and registers it as a servlet filter.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    // Constructor injection — the preferred way to inject dependencies in Spring.
    // No @Autowired on fields (that's considered bad practice in modern Spring).
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain   // the rest of the filter chain
    ) throws ServletException, IOException {

        // Read the Authorization header
        final String authHeader = request.getHeader("Authorization");

        // If no header OR doesn't start with "Bearer ", skip JWT processing.
        // The request will either be allowed (public route) or rejected (SecurityConfig).
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // pass to next filter
            return;
        }

        // Extract the token (remove "Bearer " prefix — 7 characters)
        final String jwt = authHeader.substring(7);
        final String username;

        try {
            username = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            // Token is malformed or tampered — log and let the request fail naturally
            log.debug("Invalid JWT token: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // If we got a username AND no authentication is set yet for this request
        // (SecurityContextHolder is empty = user not yet authenticated this request)
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Load the full user object from the database
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Validate: does the token belong to this user AND is it not expired?
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // Create an Authentication token — this is what Spring Security
                // uses internally to represent "who is logged in right now"
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,        // principal (the user object)
                                null,               // credentials (null — we use JWT, not password here)
                                userDetails.getAuthorities() // roles: [ROLE_USER]
                        );

                // Attach request details (IP, session) to the auth token
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // STORE in SecurityContext — this makes the user "authenticated"
                // for the rest of this request's lifecycle.
                // Any code downstream can call:
                //   SecurityContextHolder.getContext().getAuthentication()
                // and get this user object back.
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("Authenticated user: {}", username);
            }
        }

        // Pass the request to the next filter (or the controller)
        filterChain.doFilter(request, response);
    }
}
