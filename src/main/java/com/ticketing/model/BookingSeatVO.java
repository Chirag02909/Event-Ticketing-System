package com.ticketing.model;

import jakarta.persistence.*;

/**
 * Pure join table — links a booking to the specific seats it covers.
 *
 * WHY THIS IS A SEPARATE ENTITY AND NOT @ManyToMany:
 * Spring's @ManyToMany generates a join table automatically, but gives you
 * no control over indexes, cascade behaviour, or querying. By making this
 * an explicit entity we can:
 *   1. Query "which seats are in booking X?" efficiently with a JPQL join
 *   2. Add extra columns later (e.g. seat_price_at_booking_time) without a migration
 *   3. Avoid the notorious @ManyToMany lazy-loading pitfalls
 *
 * A composite PK (bookingId + seatId) enforces uniqueness at the DB level —
 * the same seat cannot appear in the same booking twice.
 */
@Entity
@Table(
        name = "booking_seats",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_booking_seat",
                columnNames = {"booking_id", "seat_id"}
        )
)
public class BookingSeatVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    public BookingSeatVO() {}

    public BookingSeatVO(Long bookingId, Long seatId) {
        this.bookingId = bookingId;
        this.seatId = seatId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Long getSeatId() { return seatId; }
    public void setSeatId(Long seatId) { this.seatId = seatId; }
}