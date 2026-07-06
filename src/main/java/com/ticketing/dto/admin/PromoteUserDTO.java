package com.ticketing.dto.admin;

import jakarta.validation.constraints.*;

/**
 * Used by ADMIN to change a user's role.
 *
 * Example use case:
 *   PATCH /api/admin/users/{id}/role
 *   Body: { "role": "ORGANISER" }
 *
 * Only "USER" and "ORGANISER" are valid targets.
 * ADMIN cannot promote someone to ADMIN via this endpoint —
 * that must be done directly in the database (intentional friction).
 */
public class PromoteUserDTO {

    @NotBlank(message = "Role is required")
    @Pattern(
            regexp = "^(USER|ORGANISER)$",
            message = "Role must be either USER or ORGANISER"
    )
    private String role;

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}