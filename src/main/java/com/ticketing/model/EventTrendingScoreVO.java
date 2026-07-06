package com.ticketing.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Stores the pre-computed booking-velocity score for one event.
 *
 * WHY THIS IS A SEPARATE TABLE FROM EventVO:
 * EventVO represents identity and configuration — title, date, venue,
 * status. This table represents a derived, constantly-recomputed metric
 * with its own update cadence. Keeping them separate means recalculating
 * trending scores every 5 minutes never touches the EventVO row that
 * every other part of the system also reads — no extra write contention
 * on a table everything depends on.
 *
 * ONE ROW PER EVENT — eventId is the primary key, so each recalculation
 * is an upsert (merge), not an insert. There is no history of past
 * scores kept; this table only ever reflects "right now."
 *
 * velocityScore DEFINITION:
 *   (count of CONFIRMED bookings created in the last `windowHours`) / windowHours
 *
 * Only CONFIRMED bookings count — PENDING_PAYMENT bookings could be
 * created in bulk and abandoned to artificially inflate a score, and
 * CANCELLED/REFUNDED bookings represent demand that didn't materialise.
 */
@Entity
@Table(name = "event_trending_scores", indexes = {
    // The trending endpoint's entire query is ORDER BY velocity_score DESC —
    // this index makes that a fast indexed sort, not a full table scan.
    @Index(name = "idx_trending_velocity_score", columnList = "velocity_score")
})
public class EventTrendingScoreVO {

    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "velocity_score", nullable = false)
    private double velocityScore;

    /**
     * Raw count behind the score — kept for transparency/debugging.
     * e.g. "47 bookings in the last 24h" is more meaningful to a human
     * reading the data than "1.958333" alone.
     */
    @Column(name = "booking_count_in_window", nullable = false)
    private int bookingCountInWindow;

    @Column(name = "window_hours", nullable = false)
    private int windowHours;

    @Column(name = "last_computed_at", nullable = false)
    private LocalDateTime lastComputedAt;

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public double getVelocityScore() { return velocityScore; }
    public void setVelocityScore(double velocityScore) { this.velocityScore = velocityScore; }

    public int getBookingCountInWindow() { return bookingCountInWindow; }
    public void setBookingCountInWindow(int bookingCountInWindow) { this.bookingCountInWindow = bookingCountInWindow; }

    public int getWindowHours() { return windowHours; }
    public void setWindowHours(int windowHours) { this.windowHours = windowHours; }

    public LocalDateTime getLastComputedAt() { return lastComputedAt; }
    public void setLastComputedAt(LocalDateTime lastComputedAt) { this.lastComputedAt = lastComputedAt; }
}