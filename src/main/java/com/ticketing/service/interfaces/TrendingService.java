package com.ticketing.service.interfaces;

import com.ticketing.dto.trending.TrendingEventDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface TrendingService {

    /**
     * Returns the top N trending events by pre-computed velocity score.
     * Pure read — does not trigger any recalculation. The data reflects
     * whatever TrendingScoreScheduler last computed (at most a few
     * minutes stale, by design).
     */
    ResponseEntity<List<TrendingEventDTO>> getTopTrending(int limit);

    /**
     * Recomputes velocity scores for every PUBLISHED event. Called by
     * TrendingScoreScheduler on a fixed schedule. Exposed as a public
     * service method (rather than private scheduler logic) so it can
     * also be triggered manually via an admin endpoint if a recalculation
     * is needed immediately after a demo or a bulk data change.
     */
    void recalculateAllScores();
}