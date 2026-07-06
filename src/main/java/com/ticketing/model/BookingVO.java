package com.ticketing.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a user's attempt to book seats for an event.
 *
 * LIFECYCLE:
 *   PENDING_PAYMENT  → booking created, seats are HELD, awaiting payment
 *   CONFIRMED        → payment webhook received and verified, tickets issued
 *   CANCELLED        → user cancelled OR hold expired before payment
 *   PAYMENT_FAILED   → Razorpay reported payment failure
 *
 * WHY booking_ref EXISTS:
 * The booking_ref (e.g. "TKT-20240615-00042") is the human-readable
 * reference shown to users in emails and on tickets. The internal id (BIGINT)
 * is never exposed outside the system.
 *
 * WHY total_amount IS DENORMALISED HERE:
 * We store the total at booking time so price changes on the event
 * don't retroactively alter existing booking amounts. This is how
 * every real booking system works.
 */
@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_bookings_user_id", columnList = "user_id"),
        @Index(name = "idx_bookings_booking_ref", columnList = "booking_ref", unique = true)
})
public class BookingVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable unique reference. Format: TKT-{date}-{padded id}
     * Generated in BookingServiceImpl after persist so we have the id.
     * e.g. "TKT-20240615-00042"
     */
    @Column(name = "booking_ref", nullable = true, unique = true, length = 30)
    private String bookingRef;

    /**
     * PENDING_PAYMENT / CONFIRMED / CANCELLED / PAYMENT_FAILED
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING_PAYMENT";

    @Column(name = "total_seats", nullable = false)
    private int totalSeats;

    /**
     * Snapshot of the total price at booking time.
     * Sum of all seat prices selected by the user.
     */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBookingRef() { return bookingRef; }
    public void setBookingRef(String bookingRef) { this.bookingRef = bookingRef; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}