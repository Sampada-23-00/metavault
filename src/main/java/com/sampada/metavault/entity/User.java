package com.sampada.metavault.entity;

import com.sampada.metavault.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * JPA Entity = a Java class mapped to a database table.
 * Each instance of this class = one row in the "users" table.
 *
 * LOMBOK ANNOTATIONS (save us from writing 100+ lines of boilerplate):
 *   @Data          → generates getters, setters, equals(), hashCode(), toString()
 *   @Builder       → enables the builder pattern: User.builder().username("x").build()
 *   @NoArgsConstructor → generates User() — required by JPA (it needs a no-arg constructor)
 *   @AllArgsConstructor → generates User(all fields) — required by @Builder
 *
 * UserDetails is a Spring Security interface. By implementing it here, this entity
 * can be used directly as a security principal — no separate adapter class needed.
 * The 4 boolean methods (isAccountNonExpired, etc.) all return true because we
 * don't need those features right now.
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    /**
     * @Id         → marks this field as the PRIMARY KEY
     * @GeneratedValue(strategy = UUID) → JPA auto-generates a UUID before INSERT
     *
     * Why UUID instead of auto-increment Long?
     * - UUIDs are globally unique — safe to generate in the app, not just the DB
     * - No sequential IDs means you can't enumerate records (/records/1, /records/2...)
     * - Distributed systems can generate IDs without a central counter
     * - Trade-off: slightly larger (16 bytes vs 8 bytes for Long), slower index scans
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * nullable = false → NOT NULL constraint in the database
     * unique = true    → UNIQUE constraint in the database
     * length = 50      → VARCHAR(50) — prevents unreasonably long usernames
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /**
     * We never store the raw password. We store the BCrypt hash.
     * BCrypt output is always 60 characters long.
     * Naming it "passwordHash" (not "password") makes this crystal clear in code.
     */
    @Column(nullable = false, length = 60)
    private String passwordHash;

    /**
     * @Enumerated(EnumType.STRING) → stores "USER" or "ADMIN" in the DB (not 0 or 1).
     * EnumType.ORDINAL (default) stores integers — if you reorder your enum, all data
     * in the DB silently means the wrong thing. Always use EnumType.STRING.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    /**
     * @Column(updatable = false) → once set, this column is never updated by JPA.
     * LocalDateTime = Java's date+time without timezone info.
     * We use UTC everywhere (set in application.yml).
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * @PrePersist is a JPA lifecycle callback — this method runs automatically
     * just before the entity is first saved (INSERTed) into the database.
     * This ensures createdAt is always set, without any service-layer code.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Spring Security - UserDetails interface implementation
    //
    // Spring Security calls these methods to understand the user's permissions.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the list of permissions this user has.
     * We give each user one authority: "ROLE_USER" or "ROLE_ADMIN".
     * SimpleGrantedAuthority wraps a string into a GrantedAuthority object.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * Spring Security calls getPassword() to get the credential for comparison.
     * Our credential IS the BCrypt hash — the framework knows how to verify it
     * against the raw password using the PasswordEncoder bean.
     */
    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    // Spring Security uses getUsername() — already generated by @Data as getUsername()
    // but we must explicitly tell it which field to use:
    @Override
    public String getUsername() {
        return this.username;
    }

    // These return true because we haven't implemented account locking/expiry features.
    // In a production app, you'd add boolean fields like `isLocked`, `isEnabled` to the entity.
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
