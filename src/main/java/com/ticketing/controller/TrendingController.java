package com.ticketing.controller;

import com.ticketing.dto.Response;
import com.ticketing.dto.trending.TrendingEventDTO;
import com.ticketing.service.interfaces.TrendingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TrendingController {

    @Autowired
    private TrendingService trendingService;

    /**
     * Public — no JWT required. Trending events are meant to drive
     * discovery on the homepage before a user logs in, same as the
     * existing GET /api/events. Add this path to SecurityConfig's
     * permitAll() list alongside the other public event routes.
     *
     * GET /api/events/trending?limit=10
     */
    @GetMapping("/api/events/trending")
    public ResponseEntity<List<TrendingEventDTO>> getTrendingEvents(
            @RequestParam(defaultValue = "10") int limit) {
        return trendingService.getTopTrending(limit);
    }

    /**
     * ADMIN-only manual trigger — useful immediately after seeding demo
     * data or right before a live demo/interview, so you don't have to
     * wait up to 5 minutes for the next scheduled run.
     *
     * POST /api/admin/trending/recalculate
     */
    @PostMapping("/api/admin/trending/recalculate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response> recalculateNow() {

        Response response = new Response();
        trendingService.recalculateAllScores();

        response.setStatus(true);
        response.setMessage("Trending scores recalculated immediately.");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}