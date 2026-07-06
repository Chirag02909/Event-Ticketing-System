package com.ticketing.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a user's position in line for a sold-out event.
 *
 * ── WHY THIS IS NOT A FIELD ON BookingVO ───────────────────────────────
 * A waitlist entry has no seat, no payment, and often no specific seat
 * preference — the user just wants ANY seat for this event when one
 * frees up. Cramming this into BookingVO would mean half its fields are
 * meaningless for half its rows. A separate table keeps both clean.
 *
 * ── STATUS LIFECYCLE ─────────────────────────────────────────────────────
 *   WAITING   → joined the line, no seat offered yet
 *   OFFERED   → a seat has been claimed for this user, offer window open
 *   CLAIMED   → user accepted the offer and completed the hold→pay flow
 *   EXPIRED   → user did not respond within the offer window
 *   CANCELLED → user voluntarily left the waitlist before being offered
 *
 * ── FIFO ORDERING ────────────────────────────────────────────────────────
 * joinedAt is the sole ordering key. The atomic claim query (see
 * WaitlistRepository.claimNextInLine) always picks the oldest WAITING
 * row for an event — first come, first offered.
 */
@Entity
@Table(name = "waitlist_entries", indexes = {
    // Used by the atomic claim query: filter by event_id + status,
    // then order by joined_at — this index covers all three.
    @Index(name = "idx_waitlist_event_status_joined",
           columnList = "event_id, status, joined_at")
})
public class WaitlistEntryVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "status", nullable = false, length = 15)
    private String status = "WAITING";

    /**
     * When the user joined the waitlist — the FIFO sort key.
     */
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    /**
     * Set when status flips to OFFERED.
     */
    @Column(name = "offered_at")
    private LocalDateTime offeredAt;

    /**
     * The seat offered to this user. Null until status = OFFERED.
     * Once offered, this specific seat is held for this specific user —
     * reuses SeatVO's existing heldBy/heldUntil mechanism from Chunk 4.
     */
    @Column(name = "offered_seat_id")
    private Long offeredSeatId;

    /**
     * Deadline for the user to accept the offer.
     * The same @Scheduled expiry pattern from Chunk 4 checks this column.
     */
    @Column(name = "offer_expires_at")
    private LocalDateTime offerExpiresAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }

    public LocalDateTime getOfferedAt() { return offeredAt; }
    public void setOfferedAt(LocalDateTime offeredAt) { this.offeredAt = offeredAt; }

    public Long getOfferedSeatId() { return offeredSeatId; }
    public void setOfferedSeatId(Long offeredSeatId) { this.offeredSeatId = offeredSeatId; }

    public LocalDateTime getOfferExpiresAt() { return offerExpiresAt; }
    public void setOfferExpiresAt(LocalDateTime offerExpiresAt) { this.offerExpiresAt = offerExpiresAt; }
}