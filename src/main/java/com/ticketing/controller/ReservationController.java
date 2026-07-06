package com.ticketing.controller;

import com.ticketing.dto.Response;
import com.ticketing.dto.booking.BookingResponseDTO;
import com.ticketing.dto.seat.HoldSeatsDTO;
import com.ticketing.service.interfaces.ReservationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Reservation controller — all endpoints require a valid JWT (any role).
 *
 * ENDPOINTS:
 *   POST   /api/seats/hold              → hold seats + create PENDING_PAYMENT booking
 *   DELETE /api/bookings/{id}/cancel    → release held seats + cancel booking
 *   GET    /api/bookings/my             → user's booking history
 *   GET    /api/bookings/{id}           → single booking detail
 */
@RestController
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    /**
     * Hold seats and create a PENDING_PAYMENT booking.
     *
     * POST /api/seats/hold
     * Body: { "seatIds": [1, 2, 3], "eventId": 42 }
     *
     * Returns booking with totalAmount so frontend can initiate Razorpay payment.
     * Seats are held for ${ticketing.seat.hold.minutes} minutes (default: 10).
     */
    @PostMapping("/api/seats/hold")
    public ResponseEntity<BookingResponseDTO> holdSeats(
            @Valid @RequestBody HoldSeatsDTO dto,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            BookingResponseDTO err = new BookingResponseDTO();
            err.setStatus(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(err);
        }

        return reservationService.holdAndCreateBooking(dto, getEmail());
    }

    /**
     * Cancel a PENDING_PAYMENT booking and release seats back to AVAILABLE.
     * Only the booking owner can cancel. Confirmed bookings cannot be cancelled here
     * — that goes through the refund flow in Chunk 5.
     *
     * DELETE /api/bookings/{id}/cancel
     */
    @DeleteMapping("/api/bookings/{id}/cancel")
    public ResponseEntity<Response> cancelBooking(@PathVariable Long id) {
        return reservationService.releaseHeldSeats(id, getEmail());
    }

    /**
     * Get the authenticated user's full booking history.
     *
     * GET /api/bookings/my
     */
    @GetMapping("/api/bookings/my")
    public ResponseEntity<List<BookingResponseDTO>> getMyBookings() {
        return reservationService.getMyBookings(getEmail());
    }

    /**
     * Get details of a specific booking.
     * Only accessible by the booking owner.
     *
     * GET /api/bookings/{id}
     */
    @GetMapping("/api/bookings/{id}")
    public ResponseEntity<BookingResponseDTO> getBookingById(@PathVariable Long id) {
        return reservationService.getBookingById(id, getEmail());
    }

    private String getEmail() {
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        User user = (User) auth.getPrincipal();

        return user.getUsername(); // email
    }
}