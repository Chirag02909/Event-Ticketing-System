package com.ticketing.service.impl;

import com.ticketing.model.BookingVO;
import com.ticketing.model.SeatVO;
import com.ticketing.repository.BookingRepository;
import com.ticketing.repository.BookingSeatRepository;
import com.ticketing.repository.SeatRepository;
import com.ticketing.service.interfaces.SeatBroadcastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Scheduled job that runs every 60 seconds and releases
 * seat holds whose expiry time has passed.
 *
 * WHY @Component AND NOT @Service:
 * This class has no service interface — it's a background task,
 * not part of any API contract. @Component is the correct annotation.
 *
 * WHY @Transactional ON THE SCHEDULED METHOD:
 * The method loads SeatVOs, modifies them, and saves. This must
 * happen in one transaction — if saving fails halfway through,
 * all changes roll back. Without @Transactional, each save() runs
 * in its own auto-committed transaction, leaving partial state.
 *
 * WHAT HAPPENS TO PENDING_PAYMENT BOOKINGS:
 * When a hold expires, the associated booking (if any) is still in
 * PENDING_PAYMENT status. We flip it to CANCELLED here too — so the
 * user can't later pay for a booking whose seats have been released.
 * Razorpay will reject payment for a cancelled order anyway (Chunk 5).
 */
@Component
public class SeatHoldExpiryScheduler {

    @Autowired private SeatRepository        seatRepository;
    @Autowired private BookingRepository     bookingRepository;
    @Autowired private BookingSeatRepository bookingSeatRepository;

    // NEW — broadcasts seat status changes to WebSocket subscribers
    @Autowired private SeatBroadcastService seatBroadcastService;

    /**
     * Runs every 60,000 ms (60 seconds).
     * fixedRate = time between the START of each execution.
     * fixedDelay would be time between END of one and START of next.
     * fixedRate is correct here — we want regular intervals regardless
     * of how long the cleanup takes.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireStaleHolds() {
        LocalDateTime now = LocalDateTime.now();

        List<SeatVO> expiredSeats = seatRepository.findExpiredHeldSeats(now);

        if (expiredSeats.isEmpty()) {
            return; // nothing to process — exit early
        }

        System.out.println("[SeatHoldExpiryScheduler] Expiring "
            + expiredSeats.size() + " stale seat hold(s) at " + now);

        for (SeatVO seat : expiredSeats) {
            // ── Cancel the associated PENDING_PAYMENT booking ─────────────
            // Find bookings linked to this seat that are still PENDING_PAYMENT
            bookingSeatRepository.findBySeatId(seat.getId())
                .stream()
                .map(bs -> bookingRepository.findByIdAndStatus(
                    bs.getBookingId(), "PENDING_PAYMENT"))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(booking -> {
                    booking.setStatus("CANCELLED");
                    bookingRepository.save(booking);
                });

            // ── Release the seat ──────────────────────────────────────────
            seat.setStatus("AVAILABLE");
            seat.setHeldBy(null);
            seat.setHeldUntil(null);
            seatRepository.save(seat);

            // ── NEW: Broadcast the release to WebSocket subscribers ──────
            seatBroadcastService.broadcastSeatUpdate(seat, "SEAT_RELEASED");
        }

        // Chunk 6 will add: broadcast seat status changes via WebSocket here

    }
}