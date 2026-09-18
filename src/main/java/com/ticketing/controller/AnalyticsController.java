package com.ticketing.controller;

import com.ticketing.dto.analytics.AttendeeDTO;
import com.ticketing.dto.analytics.OrganiserAnalyticsDTO;
import com.ticketing.service.interfaces.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organiser")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    /**
     * GET /api/organiser/analytics
     * Summary revenue, sales, occupancy, and category breakdowns for Organiser.
     */
    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ORGANISER')")
    public ResponseEntity<OrganiserAnalyticsDTO> getOrganiserAnalytics() {
        return analyticsService.getOrganiserAnalytics(getEmail());
    }

    /**
     * GET /api/organiser/events/{id}/attendees
     * Ticket holder roster for a specific event with seat allocations.
     */
    @GetMapping("/events/{id}/attendees")
    @PreAuthorize("hasRole('ORGANISER') or hasRole('ADMIN')")
    public ResponseEntity<List<AttendeeDTO>> getEventAttendees(@PathVariable Long id) {
        return analyticsService.getEventAttendees(id, getEmail());
    }

    private String getEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ((UserDetails) auth.getPrincipal()).getUsername();
    }
}
