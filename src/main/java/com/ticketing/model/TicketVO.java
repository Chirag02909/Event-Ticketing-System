package com.ticketing.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a confirmed ticket for one seat in a confirmed booking.
 *
 * WHY TICKETS ARE SEPARATE FROM BOOKINGS:
 * A booking can be in PENDING_PAYMENT state for up to 10 minutes.
 * During that window, we must NOT issue tickets — the payment might fail.
 * Tickets are only generated inside WebhookServiceImpl when Razorpay fires
 * a "payment.captured" event and we flip the booking to CONFIRMED.
 *
 * One ticket per seat:
 *   Booking with 3 seats → 3 TicketVO rows, each with a unique ticket_number.
 *
 * TICKET NUMBER FORMAT:
 *   EVT{eventId}-BKG{bookingId}-{seatNumber}
 *   e.g. "EVT12-BKG42-A07"
 *   Unique, human-readable, printable on the physical/digital ticket.
 */
@Entity
@Table(name = "tickets", indexes = {
        @Index(name = "idx_tickets_booking_id", columnList = "booking_id"),
        @Index(name = "idx_tickets_ticket_number", columnList = "ticket_number", unique = true)
})
public class TicketVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable unique ticket identifier.
     * Format: EVT{eventId}-BKG{bookingId}-{seatNumber}
     */
    @Column(name = "ticket_number", nullable = false, unique = true, length = 50)
    private String ticketNumber;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @PrePersist
    protected void onCreate() {
        issuedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Long getSeatId() { return seatId; }
    public void setSeatId(Long seatId) { this.seatId = seatId; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
}