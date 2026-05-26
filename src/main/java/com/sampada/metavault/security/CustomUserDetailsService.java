package com.sampada.metavault.security;

import com.sampada.metavault.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserDetailsService is the bridge between Spring Security and our database.
 *
 * Spring Security calls loadUserByUsername() in two situations:
 * 1. During login: to load the user so it can compare the password hash
 * 2. In JwtAuthFilter: to reload the user from DB after extracting username from JWT
 *
 * @Transactional: wraps the method in a database transaction.
 * Needed here because User has LAZY-loaded collections — accessing them
 * outside a transaction would throw LazyInitializationException.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Load a user by username. Spring Security calls this automatically.
     * Our User entity implements UserDetails, so we return it directly.
     *
     * @throws UsernameNotFoundException if user doesn't exist —
     *         Spring Security catches this and throws BadCredentialsException
     *         (so we don't leak whether the username or password was wrong)
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username: " + username));
    }
}
