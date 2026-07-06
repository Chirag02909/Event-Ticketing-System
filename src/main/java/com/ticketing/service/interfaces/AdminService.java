package com.ticketing.service.interfaces;

import com.ticketing.dto.Response;
import com.ticketing.dto.admin.PromoteUserDTO;
import com.ticketing.dto.admin.UserSummaryDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Contract for all ADMIN-only operations.
 * All methods assume the caller has already been verified as ADMIN
 * by Spring Security before reaching the controller.
 */
public interface AdminService {

    /**
     * Returns all users, optionally filtered by role.
     * @param role null = all users, "USER" or "ORGANISER" = filtered
     */
    ResponseEntity<List<UserSummaryDTO>> getAllUsers(String role);

    /**
     * Returns a single user's details by ID.
     */
    ResponseEntity<UserSummaryDTO> getUserById(Long userId);

    /**
     * Changes a user's role (USER ↔ ORGANISER).
     * Cannot promote to ADMIN — that requires direct DB access.
     */
    ResponseEntity<Response> updateUserRole(Long userId, PromoteUserDTO promoteUserDTO);

    /**
     * Hard-deletes a user account and all associated data.
     * Used for moderation (ToS violations, fraud, etc.).
     */
    ResponseEntity<Response> deleteUser(Long userId);
}