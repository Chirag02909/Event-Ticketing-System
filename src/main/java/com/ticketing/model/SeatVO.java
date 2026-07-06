package com.ticketing.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents one physical seat for a specific event.
 *
 * ─── THE MOST IMPORTANT ENTITY IN THE SYSTEM ───────────────────────────────
 *
 * Every SeatVO row is generated when an event is published. If a venue has
 * 500 seats, publishing the event creates 500 SeatVO rows, all tied to
 * that eventId.
 *
 * THE @Version FIELD:
 * This is the entire concurrency solution. Hibernate adds this to every
 * UPDATE statement it generates:
 *
 *   UPDATE seats
 *   SET status='HELD', held_by=101, held_until='...', version=1
 *   WHERE id=55 AND version=0     ← the magic clause
 *
 * If two users read version=0 and both try to write version=1:
 *   - User 1 succeeds: 1 row affected
 *   - User 2 fails:    0 rows affected → Hibernate throws OptimisticLockException
 *
 * You catch OptimisticLockException in ReservationServiceImpl (Chunk 4)
 * and return "Seat no longer available" to User 2. Clean, no deadlocks.
 *
 * THE HOLD EXPIRY PATTERN:
 * held_until stores the expiry timestamp when status = HELD.
 * A @Scheduled job (Chunk 4) runs every 60 seconds and executes:
 *
 *   UPDATE seats SET status='AVAILABLE', held_by=NULL, held_until=NULL
 *   WHERE status='HELD' AND held_until < NOW()
 *
 * This frees seats for users who selected but never paid.
 *
 * STATUS TRANSITIONS:
 *   AVAILABLE → HELD     (user selects seats)
 *   HELD      → AVAILABLE (hold expires or user cancels)
 *   HELD      → BOOKED   (payment confirmed via webhook)
 *   BOOKED    → AVAILABLE (booking cancelled, refund processed)
 * ────────────────────────────────────────────────────────────────────────────
 */
@Entity
@Table(name = "seats", indexes = {
        // Index on event_id — every seat query filters by event first
        @Index(name = "idx_seats_event_id", columnList = "event_id"),
        // Index on status + held_until — used by the expiry scheduler query
        @Index(name = "idx_seats_status_held_until", columnList = "status, held_until")
})
public class SeatVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable seat identifier, e.g. "A12", "B07", "VIP-3"
     * Displayed on the seat map and printed on tickets.
     */
    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    /**
     * Row label — e.g. "A", "B", "VIP"
     * Used by the frontend to group seats into rows on the seat map.
     */
    @Column(name = "row_label", nullable = false, length = 5)
    private String rowLabel;

    /**
     * Pricing category — "GENERAL", "PREMIUM", "VIP"
     * Determines price and is displayed as a colour zone on the seat map.
     */
    @Column(name = "category", nullable = false, length = 20)
    private String category;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * AVAILABLE → HELD → BOOKED (primary status machine)
     * See class Javadoc for full transition table.
     */
    @Column(name = "status", nullable = false, length = 15)
    private String status = "AVAILABLE";

    /**
     * FK to users.id — set when the seat is HELD so we know who is holding it.
     * Cleared when hold expires or booking is confirmed.
     */
    @Column(name = "held_by")
    private Long heldBy;

    /**
     * Timestamp when the current HELD status expires.
     * The @Scheduled expiry job compares every seat's held_until against NOW().
     * Null when status is AVAILABLE or BOOKED.
     */
    @Column(name = "held_until")
    private LocalDateTime heldUntil;

    /**
     * ── OPTIMISTIC LOCKING COLUMN ──
     * Managed entirely by Hibernate — never set this manually.
     * Hibernate reads it on SELECT and appends "AND version = ?" on every UPDATE.
     * If the version in the DB doesn't match what Hibernate read, the update
     * returns 0 rows affected and Hibernate throws OptimisticLockException.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Every seat belongs to a specific event.
     * This is intentionally NOT a @ManyToOne — we use a plain FK to avoid
     * Hibernate accidentally joining the events table on every seat query.
     */
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

    public String getRowLabel() { return rowLabel; }
    public void setRowLabel(String rowLabel) { this.rowLabel = rowLabel; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getHeldBy() { return heldBy; }
    public void setHeldBy(Long heldBy) { this.heldBy = heldBy; }

    public LocalDateTime getHeldUntil() { return heldUntil; }
    public void setHeldUntil(LocalDateTime heldUntil) { this.heldUntil = heldUntil; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
}