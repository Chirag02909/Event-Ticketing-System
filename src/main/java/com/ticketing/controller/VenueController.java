package com.ticketing.controller;

import com.ticketing.dto.event.CreateVenueDTO;
import com.ticketing.dto.event.VenueResponseDTO;
import com.ticketing.service.interfaces.VenueService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Venue management — ORGANISER only.
 * All routes are under /api/organiser/venues which SecurityConfig
 * already protects with hasRole('ORGANISER').
 */
@RestController
@RequestMapping("/api/organiser/venues")
@PreAuthorize("hasRole('ORGANISER')")
public class VenueController {

    @Autowired
    private VenueService venueService;

    /**
     * POST /api/organiser/venues
     * Create a new venue. Organiser ID resolved from JWT.
     */
    @PostMapping
    public ResponseEntity<VenueResponseDTO> createVenue(
            @Valid @RequestBody CreateVenueDTO dto,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            VenueResponseDTO err = new VenueResponseDTO();
            err.setName(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(err);
        }

        return venueService.createVenue(dto, getEmail());
    }

    /**
     * GET /api/organiser/venues
     * List all venues created by this organiser.
     */
    @GetMapping
    public ResponseEntity<List<VenueResponseDTO>> getMyVenues() {
        return venueService.getMyVenues(getEmail());
    }

    /**
     * GET /api/organiser/venues/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<VenueResponseDTO> getVenueById(@PathVariable Long id) {
        return venueService.getVenueById(id);
    }

    private String getEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}