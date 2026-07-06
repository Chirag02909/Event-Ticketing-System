package com.ticketing.controller;

import com.ticketing.dto.Response;
import com.ticketing.dto.booking.BookingResponseDTO;
import com.ticketing.dto.waitlist.JoinWaitlistDTO;
import com.ticketing.dto.waitlist.WaitlistResponseDTO;
import com.ticketing.service.interfaces.WaitlistService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Waitlist controller — JWT required, any authenticated user.
 * Falls under the existing /api/bookings/** style authenticated rule
 * pattern; routes are under /api/waitlist for clarity.
 */
@RestController
@RequestMapping("/api/waitlist")
public class WaitlistController {

    @Autowired
    private WaitlistService waitlistService;

    /**
     * Join the waitlist for a sold-out event.
     *
     * POST /api/waitlist
     * Body: { "eventId": 42 }
     */
    @PostMapping
    public ResponseEntity<WaitlistResponseDTO> joinWaitlist(
            @Valid @RequestBody JoinWaitlistDTO dto,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                .body(new WaitlistResponseDTO(false,
                    bindingResult.getFieldErrors().get(0).getDefaultMessage()));
        }

        return waitlistService.joinWaitlist(dto, getEmail());
    }

    /**
     * Leave the waitlist before being offered a seat (or give up an offer).
     *
     * DELETE /api/waitlist/{entryId}
     */
    @DeleteMapping("/{entryId}")
    public ResponseEntity<Response> leaveWaitlist(@PathVariable Long entryId) {
        return waitlistService.leaveWaitlist(entryId, getEmail());
    }

    /**
     * Check status and position-in-line for a specific entry.
     *
     * GET /api/waitlist/{entryId}
     */
    @GetMapping("/{entryId}")
    public ResponseEntity<WaitlistResponseDTO> getStatus(@PathVariable Long entryId) {
        return waitlistService.getStatus(entryId, getEmail());
    }

    /**
     * List all of the authenticated user's waitlist entries.
     *
     * GET /api/waitlist/my
     */
    @GetMapping("/my")
    public ResponseEntity<List<WaitlistResponseDTO>> getMyEntries() {
        return waitlistService.getMyWaitlistEntries(getEmail());
    }

    /**
     * Accept an OFFERED seat — converts the waitlist entry into a real
     * held booking, continuing through the existing pay flow from Chunk 5.
     *
     * POST /api/waitlist/{entryId}/accept
     */
    @PostMapping("/{entryId}/accept")
    public ResponseEntity<BookingResponseDTO> acceptOffer(@PathVariable Long entryId) {
        return waitlistService.acceptOffer(entryId, getEmail());
    }

    private String getEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}