package com.ticketing.service.interfaces;

import com.ticketing.dto.booking.BookingResponseDTO;
import com.ticketing.dto.seat.HoldSeatsDTO;
import com.ticketing.dto.Response;
import org.springframework.http.ResponseEntity;

public interface ReservationService {

    /**
     * Holds requested seats using optimistic locking, then creates
     * a PENDING_PAYMENT booking. Returns the booking details including
     * totalAmount so the frontend can initiate payment.
     *
     * Throws a clean error if any seat is already HELD or BOOKED,
     * or if OptimisticLockException fires (concurrent selection).
     */
    ResponseEntity<BookingResponseDTO> holdAndCreateBooking(
        HoldSeatsDTO dto, String userEmail
    );

    /**
     * Releases held seats back to AVAILABLE.
     * Called when user explicitly cancels before paying,
     * or when the scheduler finds expired holds.
     */
    ResponseEntity<Response> releaseHeldSeats(Long bookingId, String userEmail);

    /**
     * Fetches a user's booking history.
     */
    ResponseEntity<java.util.List<BookingResponseDTO>> getMyBookings(String userEmail);

    /**
     * Fetches a single booking by ID.
     */
    ResponseEntity<BookingResponseDTO> getBookingById(Long bookingId, String userEmail);
}