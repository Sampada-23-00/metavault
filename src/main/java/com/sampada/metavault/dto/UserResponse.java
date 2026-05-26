package com.sampada.metavault.dto;

import com.sampada.metavault.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Returned by GET /api/auth/me — the current user's profile.
 * Notice: no passwordHash field. We never send password data in responses.
 */
@Data
@Builder
public class UserResponse {
    private UUID id;
    private String username;
    private String email;
    private Role role;
    private LocalDateTime createdAt;
}
