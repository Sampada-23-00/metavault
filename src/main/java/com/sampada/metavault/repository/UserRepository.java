package com.sampada.metavault.repository;

import com.sampada.metavault.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository — the magic interface.
 *
 * JpaRepository<User, UUID> means:
 *   - Entity type: User
 *   - Primary key type: UUID
 *
 * By extending JpaRepository, Spring Data AUTO-GENERATES implementations
 * for all common operations at startup:
 *   - save(user)           → INSERT or UPDATE
 *   - findById(id)         → SELECT WHERE id = ?
 *   - findAll()            → SELECT *
 *   - delete(user)         → DELETE
 *   - existsById(id)       → SELECT count(*) WHERE id = ?
 *   ...and ~15 more
 *
 * You write ZERO implementation code — just the interface declaration.
 * Spring reads the method names and generates SQL from them:
 *   findByUsername → SELECT * FROM users WHERE username = ?
 *   existsByEmail  → SELECT count(*) > 0 FROM users WHERE email = ?
 *
 * Optional<User> means the query might return nothing (user not found).
 * Callers must handle the empty case, preventing NullPointerExceptions.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
