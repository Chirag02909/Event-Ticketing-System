package com.ticketing.controller;

import com.ticketing.dto.admin.PromoteUserDTO;
import com.ticketing.dto.admin.UserSummaryDTO;
import com.ticketing.dto.*;
import com.ticketing.service.interfaces.AdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ADMIN-only controller.
 *
 * DOUBLE PROTECTION:
 *   Layer 1 → SecurityConfig:  .requestMatchers("/api/admin/**").hasRole("ADMIN")
 *   Layer 2 → @PreAuthorize:   @PreAuthorize("hasRole('ADMIN')") on each method
 *
 * Both layers must pass. This is defence in depth — if SecurityConfig is
 * ever accidentally modified, @PreAuthorize catches it at the method level.
 *
 * ENDPOINTS:
 *   GET    /api/admin/users              → list all users (optional ?role= filter)
 *   GET    /api/admin/users/{id}         → get single user
 *   PATCH  /api/admin/users/{id}/role    → change user role (USER ↔ ORGANISER)
 *   DELETE /api/admin/users/{id}         → delete user account
 *
 * Further admin endpoints for events and payments are added in Chunks 3 and 5.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")          // method-level guard on all endpoints
public class AdminController {

    @Autowired
    private AdminService adminService;

    /**
     * List all users. Optional query param ?role=USER or ?role=ORGANISER to filter.
     *
     * GET /api/admin/users
     * GET /api/admin/users?role=ORGANISER
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserSummaryDTO>> getAllUsers(
            @RequestParam(required = false) String role) {
        return adminService.getAllUsers(role);
    }

    /**
     * Get a specific user by their internal ID.
     *
     * GET /api/admin/users/{id}
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<UserSummaryDTO> getUserById(@PathVariable Long id) {
        return adminService.getUserById(id);
    }

    /**
     * Change a user's role between USER and ORGANISER.
     * Cannot promote to ADMIN — intentionally blocked.
     *
     * PATCH /api/admin/users/{id}/role
     * Body: { "role": "ORGANISER" }
     */
    @PatchMapping("/users/{id}/role")
    public ResponseEntity<Response> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody PromoteUserDTO promoteUserDTO) {
        return adminService.updateUserRole(id, promoteUserDTO);
    }

    /**
     * Hard-delete a user account (moderation use — ToS violations, fraud).
     * Cannot delete ADMIN accounts.
     *
     * DELETE /api/admin/users/{id}
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Response> deleteUser(@PathVariable Long id) {
        return adminService.deleteUser(id);
    }
}