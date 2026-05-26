package com.sampada.metavault.dto;

import com.sampada.metavault.enums.Role;
import lombok.Builder;
import lombok.Data;

/**
 * What we return after successful register or login.
 * The client stores the `token` and sends it on every future request
 * as: Authorization: Bearer <token>
 */
@Data
@Builder
public class AuthResponse {

    private String token;

    // Always "Bearer" — tells the client what type of token this is
    @Builder.Default
    private String tokenType = "Bearer";

    private String username;
    private String email;
    private Role role;
}
