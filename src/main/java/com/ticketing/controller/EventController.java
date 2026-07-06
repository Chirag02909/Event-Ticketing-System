package com.ticketing.controller;

import com.ticketing.dto.Response;
import com.ticketing.dto.event.CreateEventDTO;
import com.ticketing.dto.event.EventResponseDTO;
import com.ticketing.dto.seat.SeatResponseDTO;
import com.ticketing.service.interfaces.EventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class EventController {

    @Autowired
    private EventService eventService;

    // ─────────────────────────────────────────────────────────────────────
    // PUBLIC — no JWT required (permitted in SecurityConfig)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * GET /api/events
     * Browse all published events. Open to everyone.
     */
    @GetMapping("/api/events")
    public ResponseEntity<List<EventResponseDTO>> getPublishedEvents() {
        return eventService.getPublishedEvents();
    }

    /**
     * GET /api/events/{id}
     * View a single event's details. Open to everyone.
     */
    @GetMapping("/api/events/{id}")
    public ResponseEntity<EventResponseDTO> getEventById(@PathVariable Long id) {
        return eventService.getEventById(id);
    }

    /**
     * GET /api/events/{id}/seats
     * Live seat map for an event. Open to everyone so users can
     * browse availability before deciding to log in and book.
     */
    @GetMapping("/api/events/{id}/seats")
    public ResponseEntity<List<SeatResponseDTO>> getSeatMap(@PathVariable Long id) {
        return eventService.getSeatMap(id);
    }

    // ─────────────────────────────────────────────────────────────────────
    // ORGANISER — JWT required, ORGANISER role
    // ─────────────────────────────────────────────────────────────────────

    /**
     * POST /api/organiser/events
     * Create a new event in DRAFT status.
     */
    @PostMapping("/api/organiser/events")
    @PreAuthorize("hasRole('ORGANISER')")
    public ResponseEntity<EventResponseDTO> createEvent(
            @Valid @RequestBody CreateEventDTO dto,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                .body(buildErrorResponse(
                    bindingResult.getFieldErrors().get(0).getDefaultMessage()));
        }

        return eventService.createEvent(dto, getEmail());
    }

    /**
     * PATCH /api/organiser/events/{id}/publish
     * Publish a DRAFT event — triggers bulk seat generation.
     */
    @PatchMapping("/api/organiser/events/{id}/publish")
    @PreAuthorize("hasRole('ORGANISER')")
    public ResponseEntity<EventResponseDTO> publishEvent(@PathVariable Long id) {
        return eventService.publishEvent(id, getEmail());
    }

    /**
     * GET /api/organiser/events
     * Organiser's own event list — all statuses including DRAFT.
     */
    @GetMapping("/api/organiser/events")
    @PreAuthorize("hasRole('ORGANISER')")
    public ResponseEntity<List<EventResponseDTO>> getMyEvents() {
        return eventService.getMyEvents(getEmail());
    }

    /**
     * PATCH /api/organiser/events/{id}/cancel
     * Cancel an event. ORGANISER can cancel their own events.
     * ADMIN can cancel any event (checked in service layer).
     */
    @PatchMapping("/api/organiser/events/{id}/cancel")
    @PreAuthorize("hasRole('ORGANISER') or hasRole('ADMIN')")
    public ResponseEntity<Response> cancelEvent(@PathVariable Long id) {
        return eventService.cancelEvent(id, getEmail());
    }

    // ─────────────────────────────────────────────────────────────────────
    // ADMIN — JWT required, ADMIN role
    // ─────────────────────────────────────────────────────────────────────

    /**
     * GET /api/admin/events
     * All events across all organisers. Optional ?status= filter.
     */
    @GetMapping("/api/admin/events")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EventResponseDTO>> getAllEventsAdmin(
            @RequestParam(required = false) String status) {
        return eventService.getAllEventsAdmin(status);
    }

    // ── Private helper ────────────────────────────────────────────────────

    private String getEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ((UserDetails) auth.getPrincipal()).getUsername();
    }

    private EventResponseDTO buildErrorResponse(String message) {
        EventResponseDTO dto = new EventResponseDTO();
        dto.setTitle(message);
        return dto;
    }
}