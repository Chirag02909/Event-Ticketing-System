package com.ticketing.dto.payment;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for POST /api/payments/create-order.
 * The bookingId links this payment to an existing PENDING_PAYMENT booking.
 */
public class CreateOrderDTO {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
}