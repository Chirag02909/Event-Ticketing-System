package com.ticketing.service.impl;

import com.ticketing.dto.booking.BookingResponseDTO;
import com.ticketing.dto.seat.HoldSeatsDTO;
import com.ticketing.dto.seat.SeatResponseDTO;
import com.ticketing.dto.Response;
import com.ticketing.model.*;
import com.ticketing.repository.*;
import com.ticketing.service.interfaces.ReservationService;
import com.ticketing.service.interfaces.SeatBroadcastService;
import jakarta.persistence.OptimisticLockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReservationServiceImpl implements ReservationService {

    @Value("${ticketing.seat.hold.minutes:10}")
    private int holdMinutes;

    @Autowired private SeatRepository        seatRepository;
    @Autowired private BookingRepository     bookingRepository;
    @Autowired private BookingSeatRepository bookingSeatRepository;
    @Autowired private EventRepository       eventRepository;
    @Autowired private UserDAO        userRepository;

    @Autowired private SeatBroadcastService seatBroadcastService;

    // ─────────────────────────────────────────────────────────────────────
    // HOLD SEATS + CREATE BOOKING
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<BookingResponseDTO> holdAndCreateBooking(
            HoldSeatsDTO dto, String userEmail) {

        // ── Resolve user ──────────────────────────────────────────────────
        Optional<UserVO> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        UserVO user = userOpt.get();

        // ── Validate event is still PUBLISHED ────────────────────────────
        Optional<EventVO> eventOpt = eventRepository.findById(dto.getEventId());
        if (eventOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorResponse("Event not found."));
        }
        if (!"PUBLISHED".equals(eventOpt.get().getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse("This event is no longer available for booking."));
        }

        // ── Load and validate each requested seat ─────────────────────────
        List<SeatVO> seats = new ArrayList<>();
        for (Long seatId : dto.getSeatIds()) {
            Optional<SeatVO> seatOpt = seatRepository.findById(seatId);

            if (seatOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorResponse("Seat ID " + seatId + " not found."));
            }

            SeatVO seat = seatOpt.get();

            // Seat must belong to the requested event
            if (!seat.getEventId().equals(dto.getEventId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorResponse("Seat " + seat.getSeatNumber()
                        + " does not belong to this event."));
            }

            // Seat must be AVAILABLE — reject HELD or BOOKED immediately
            if (!"AVAILABLE".equals(seat.getStatus())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(errorResponse("Seat " + seat.getSeatNumber()
                        + " is no longer available."));
            }

            seats.add(seat);
        }

        // ── OPTIMISTIC LOCKING — hold each seat ───────────────────────────
        //
        // This is where the race condition is handled.
        //
        // Hibernate generates:
        //   UPDATE seats SET status='HELD', held_by=?, held_until=?, version=N+1
        //   WHERE id=? AND version=N
        //
        // If two users try to hold the same seat simultaneously:
        //   User 1: version=0 → UPDATE WHERE version=0 → 1 row affected ✓
        //   User 2: version=0 → UPDATE WHERE version=0 → 0 rows affected
        //                       → Hibernate throws OptimisticLockException ✗
        //
        // We catch the exception below and return a clean 409 CONFLICT.
        // ─────────────────────────────────────────────────────────────────
        LocalDateTime holdExpiry = LocalDateTime.now().plusMinutes(holdMinutes);

        try {
            for (SeatVO seat : seats) {
                seat.setStatus("HELD");
                seat.setHeldBy(user.getId());
                seat.setHeldUntil(holdExpiry);
                seatRepository.save(seat);
            }
            // Force Hibernate to flush all updates to DB within this transaction.
            // This triggers the version check NOW, inside the try block,
            // so OptimisticLockException is catchable here.
            seatRepository.flush();

        } catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
            // Another user grabbed one of these seats between our read and write.
            // Return a clean conflict response — no stack trace exposed.
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorResponse(
                    "One or more seats were just taken by another user. " +
                    "Please refresh the seat map and try again."));
        }

        // ── NEW: Broadcast each held seat to WebSocket subscribers ────────
        // Runs AFTER the successful flush — so we only broadcast confirmed
        // state changes, never state that might still get rolled back.
        for (SeatVO seat : seats) {
            seatBroadcastService.broadcastSeatUpdate(seat, "SEAT_HELD");
        }

        // ── Create the booking ─────────────────────────────────────────────
        BigDecimal totalAmount = seats.stream()
            .map(SeatVO::getPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BookingVO booking = new BookingVO();
        booking.setUserId(user.getId());
        booking.setEventId(dto.getEventId());
        booking.setTotalSeats(seats.size());
        booking.setTotalAmount(totalAmount);
        booking.setStatus("PENDING_PAYMENT");
        BookingVO savedBooking = bookingRepository.save(booking);

        // Generate human-readable booking ref after we have the ID
        // Format: TKT-20240615-00042
        String bookingRef = generateBookingRef(savedBooking.getId());
        savedBooking.setBookingRef(bookingRef);
        bookingRepository.save(savedBooking);

        // ── Link seats to booking via BookingSeatVO ────────────────────────
        for (SeatVO seat : seats) {
            bookingSeatRepository.save(
                new BookingSeatVO(savedBooking.getId(), seat.getId())
            );
        }

        // ── Build response ─────────────────────────────────────────────────
        EventVO event = eventOpt.get();
        List<SeatResponseDTO> seatDTOs = seats.stream()
            .map(this::toSeatDTO)
            .collect(Collectors.toList());

        BookingResponseDTO response = new BookingResponseDTO();
        response.setId(savedBooking.getId());
        response.setBookingRef(savedBooking.getBookingRef());
        response.setStatus(savedBooking.getStatus());
        response.setTotalSeats(savedBooking.getTotalSeats());
        response.setTotalAmount(savedBooking.getTotalAmount());
        response.setEventId(event.getId());
        response.setEventTitle(event.getTitle());
        response.setEventDate(event.getEventDate());
        response.setSeats(seatDTOs);
        response.setCreatedAt(savedBooking.getCreatedAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─────────────────────────────────────────────────────────────────────
    // RELEASE HELD SEATS (user cancels before paying)
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<Response> releaseHeldSeats(
            Long bookingId, String userEmail) {

        Response response = new Response();

        Optional<UserVO> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            response.setStatus(false);
            response.setMessage("User not found.");

            return new ResponseEntity(response, HttpStatus.NOT_FOUND);
        }

        Optional<BookingVO> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            response.setStatus(false);
            response.setMessage("Booking not found.");

            return new ResponseEntity(response, HttpStatus.NOT_FOUND);
        }

        BookingVO booking = bookingOpt.get();

// Only booking owner can cancel
        if (!booking.getUserId().equals(userOpt.get().getId())) {
            response.setStatus(false);
            response.setMessage("You are not authorised to cancel this booking.");

            return new ResponseEntity(response, HttpStatus.FORBIDDEN);
        }

// Only pending bookings can be cancelled
        if (!"PENDING_PAYMENT".equals(booking.getStatus())) {
            response.setStatus(false);
            response.setMessage(
                    "Only pending bookings can be cancelled. Current status: "
                            + booking.getStatus());

            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        }

        // Release all seats linked to this booking
        List<BookingSeatVO> links = bookingSeatRepository.findByBookingId(bookingId);
        for (BookingSeatVO link : links) {
            seatRepository.findById(link.getSeatId()).ifPresent(seat -> {
                seat.setStatus("AVAILABLE");
                seat.setHeldBy(null);
                seat.setHeldUntil(null);
                seatRepository.save(seat);
            });
        }

        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);

        response.setStatus(true);
        response.setMessage("Booking cancelled. Seats released.");

        return new ResponseEntity(response, HttpStatus.OK);
    }

    // ─────────────────────────────────────────────────────────────────────
    // READ — booking history
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<List<BookingResponseDTO>> getMyBookings(String userEmail) {
        Optional<UserVO> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<BookingResponseDTO> bookings = bookingRepository
            .findByUserId(userOpt.get().getId())
            .stream()
            .map(this::toBookingDTO)
            .collect(Collectors.toList());

        return ResponseEntity.ok(bookings);
    }

    @Override
    public ResponseEntity<BookingResponseDTO> getBookingById(
            Long bookingId, String userEmail) {

        Optional<UserVO> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Optional<BookingVO> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        BookingVO booking = bookingOpt.get();

        if (!booking.getUserId().equals(userOpt.get().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(toBookingDTO(booking));
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Generates a human-readable booking reference.
     * Format: TKT-{YYYYMMDD}-{zero-padded id}
     * Example: TKT-20240615-00042
     */
    private String generateBookingRef(Long bookingId) {
        String date = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "TKT-" + date + "-" + String.format("%05d", bookingId);
    }

    private BookingResponseDTO toBookingDTO(BookingVO booking) {
        BookingResponseDTO dto = new BookingResponseDTO();
        dto.setId(booking.getId());
        dto.setBookingRef(booking.getBookingRef());
        dto.setStatus(booking.getStatus());
        dto.setTotalSeats(booking.getTotalSeats());
        dto.setTotalAmount(booking.getTotalAmount());
        dto.setEventId(booking.getEventId());
        dto.setCreatedAt(booking.getCreatedAt());

        // Resolve event details
        eventRepository.findById(booking.getEventId()).ifPresent(event -> {
            dto.setEventTitle(event.getTitle());
            dto.setEventDate(event.getEventDate());
        });

        // Resolve seat details
        List<SeatResponseDTO> seats = bookingSeatRepository
            .findByBookingId(booking.getId())
            .stream()
            .map(bs -> seatRepository.findById(bs.getSeatId())
                .map(this::toSeatDTO).orElse(null))
            .filter(s -> s != null)
            .collect(Collectors.toList());
        dto.setSeats(seats);

        return dto;
    }

    private SeatResponseDTO toSeatDTO(SeatVO seat) {
        SeatResponseDTO dto = new SeatResponseDTO();
        dto.setId(seat.getId());
        dto.setSeatNumber(seat.getSeatNumber());
        dto.setRowLabel(seat.getRowLabel());
        dto.setCategory(seat.getCategory());
        dto.setPrice(seat.getPrice());
        dto.setStatus(seat.getStatus());
        return dto;
    }

    private BookingResponseDTO errorResponse(String message) {
        BookingResponseDTO dto = new BookingResponseDTO();
        dto.setStatus(message);
        return dto;
    }
}