package com.ticketing.service.impl;

import com.ticketing.dto.trending.TrendingEventDTO;
import com.ticketing.model.EventTrendingScoreVO;
import com.ticketing.model.EventVO;
import com.ticketing.model.VenueVO;
import com.ticketing.repository.BookingRepository;
import com.ticketing.repository.EventRepository;
import com.ticketing.repository.EventTrendingScoreRepository;
import com.ticketing.repository.SeatRepository;
import com.ticketing.repository.VenueRepository;
import com.ticketing.service.interfaces.TrendingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class TrendingServiceImpl implements TrendingService {

    @Value("${ticketing.trending.window-hours:24}")
    private int windowHours;

    @Autowired private BookingRepository              bookingRepository;
    @Autowired private EventRepository                eventRepository;
    @Autowired private VenueRepository                venueRepository;
    @Autowired private SeatRepository                 seatRepository;
    @Autowired private EventTrendingScoreRepository   trendingScoreRepository;

    // ─────────────────────────────────────────────────────────────────────
    // READ — the cheap path, just reads pre-computed scores
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<List<TrendingEventDTO>> getTopTrending(int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(1, Math.min(limit, 50)));

        List<EventTrendingScoreVO> scores = trendingScoreRepository.findTopTrending(pageable);

        List<TrendingEventDTO> result = scores.stream()
            .map(this::toDTO)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────
    // RECALCULATE — the scheduled aggregation, runs every 5 minutes
    // ─────────────────────────────────────────────────────────────────────

    /**
     * THE CORE ALGORITHM:
     *   1. One aggregation query: how many CONFIRMED bookings did each
     *      event get in the last `windowHours` hours? (single GROUP BY,
     *      not N queries for N events)
     *   2. For every PUBLISHED event — including ones with ZERO recent
     *      bookings, which need their score reset to 0 rather than left
     *      stale from a previous run — upsert a row into
     *      event_trending_scores.
     *
     * WHY EVERY PUBLISHED EVENT, NOT JUST ONES WITH BOOKINGS:
     * If event X had a burst of bookings 2 hours ago and then nothing,
     * its score must decay back toward 0 as the window rolls forward.
     * Only updating events that appear in the aggregation result would
     * leave X's last-computed high score frozen forever, which defeats
     * the entire purpose of a "rolling window."
     */
    @Override
    public void recalculateAllScores() {
        LocalDateTime since = LocalDateTime.now().minusHours(windowHours);

        // Step 1: one aggregation query for ALL events with recent activity
        List<Object[]> rawCounts = bookingRepository.countConfirmedBookingsPerEventSince(since);

        Map<Long, Integer> countsByEvent = new HashMap<>();
        for (Object[] row : rawCounts) {
            Long eventId = (Long) row[0];
            Long count = (Long) row[1];
            countsByEvent.put(eventId, count.intValue());
        }

        // Step 2: upsert a score row for every PUBLISHED event
        List<EventVO> publishedEvents = eventRepository.findByStatus("PUBLISHED");

        LocalDateTime now = LocalDateTime.now();

        for (EventVO event : publishedEvents) {
            int count = countsByEvent.getOrDefault(event.getId(), 0);
            double velocity = (double) count / windowHours;

            EventTrendingScoreVO score = trendingScoreRepository
                .findById(event.getId())
                .orElseGet(EventTrendingScoreVO::new);

            score.setEventId(event.getId());
            score.setVelocityScore(velocity);
            score.setBookingCountInWindow(count);
            score.setWindowHours(windowHours);
            score.setLastComputedAt(now);

            trendingScoreRepository.save(score);
        }

        System.out.println("[TrendingService] Recalculated velocity scores for "
            + publishedEvents.size() + " published event(s) using a "
            + windowHours + "-hour window.");
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRIVATE — mapper
    // ─────────────────────────────────────────────────────────────────────

    private Optional<TrendingEventDTO> toDTO(EventTrendingScoreVO score) {
        Optional<EventVO> eventOpt = eventRepository.findById(score.getEventId());
        if (eventOpt.isEmpty()) {
            // Event was deleted/cancelled since the last score computation —
            // skip it rather than returning a broken entry. The next
            // scheduled run will clean this up by no longer upserting it.
            return Optional.empty();
        }

        EventVO event = eventOpt.get();

        TrendingEventDTO dto = new TrendingEventDTO();
        dto.setEventId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setEventDate(event.getEventDate());
        dto.setVelocityScore(score.getVelocityScore());
        dto.setBookingCountInWindow(score.getBookingCountInWindow());
        dto.setWindowHours(score.getWindowHours());

        venueRepository.findById(event.getVenueId()).ifPresent(venue -> {
            dto.setVenueName(venue.getName());
            dto.setVenueCity(venue.getCity());
        });

        long available = seatRepository.countByEventIdAndStatus(event.getId(), "AVAILABLE");
        dto.setAvailableSeats((int) available);

        return Optional.of(dto);
    }
}