package com.ticketing.service.impl;

import com.ticketing.service.interfaces.TrendingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Recomputes trending velocity scores for every published event on a
 * fixed schedule.
 *
 * Same @Scheduled pattern as SeatHoldExpiryScheduler (Chunk 4) and
 * WaitlistOfferExpiryScheduler (Chunk 9) — a @Component with one
 * @Scheduled method, delegating the actual work to a service so the
 * logic stays testable independent of the scheduling mechanism.
 *
 * 5-minute interval is a deliberate choice: frequent enough that
 * "trending" feels responsive within a single browsing session, but
 * infrequent enough that the aggregation query (one GROUP BY across
 * all bookings in the window) never runs so often it competes with
 * real booking traffic for database resources.
 */
@Component
public class TrendingScoreScheduler {

    @Autowired
    private TrendingService trendingService;

    @Scheduled(fixedRate = 300000) // 5 minutes
    public void recalculateTrendingScores() {
        trendingService.recalculateAllScores();
    }
}