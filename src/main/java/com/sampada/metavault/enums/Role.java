package com.sampada.metavault.enums;

/**
 * Roles control what a user can do in the system.
 *
 * In Spring Security, roles are prefixed with "ROLE_" internally.
 * When we say hasRole("USER"), Spring checks for "ROLE_USER".
 * We store just "USER" / "ADMIN" in the database.
 *
 * Why an enum? Because a role is a closed set — there are exactly
 * 2 valid values. Using a String would allow typos like "ADMN".
 * JPA stores enums as either ordinal (0, 1 — fragile) or string ("USER").
 * We'll use STRING via @Enumerated(EnumType.STRING) on the entity field.
 */
public enum Role {
    USER,
    ADMIN
}
