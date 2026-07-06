package com.ticketing.service.impl;

import com.ticketing.model.SeatVO;
import com.ticketing.model.WaitlistEntryVO;
import com.ticketing.repository.SeatRepository;
import com.ticketing.repository.WaitlistRepository;
import com.ticketing.service.interfaces.SeatBroadcastService;
import com.ticketing.service.interfaces.WaitlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job that runs every 60 seconds and expires waitlist offers
 * whose response window has passed — then cascades the freed seat to
 * the NEXT person in line.
 *
 * This is the same @Scheduled pattern as SeatHoldExpiryScheduler from
 * Chunk 4, applied one layer up: instead of just releasing a seat back
 * to AVAILABLE, an expired waitlist offer must also trigger
 * offerNextInLine() again, so the line keeps moving instead of stalling
 * every time someone ignores their offer.
 */
@Component
public class WaitlistOfferExpiryScheduler {

    @Autowired private WaitlistRepository    waitlistRepository;
    @Autowired private SeatRepository        seatRepository;
    @Autowired private SeatBroadcastService  seatBroadcastService;
    @Autowired private WaitlistService       waitlistService;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireStaleOffers() {
        LocalDateTime now = LocalDateTime.now();

        List<WaitlistEntryVO> expiredOffers = waitlistRepository.findExpiredOffers(now);

        if (expiredOffers.isEmpty()) {
            return;
        }

        System.out.println("[WaitlistOfferExpiryScheduler] Expiring "
            + expiredOffers.size() + " stale waitlist offer(s) at " + now);

        for (WaitlistEntryVO entry : expiredOffers) {
            // Release the seat that was held for this specific user
            if (entry.getOfferedSeatId() != null) {
                seatRepository.findById(entry.getOfferedSeatId()).ifPresent(seat -> {
                    seat.setStatus("AVAILABLE");
                    seat.setHeldBy(null);
                    seat.setHeldUntil(null);
                    seatRepository.save(seat);
                    seatBroadcastService.broadcastSeatUpdate(seat, "SEAT_RELEASED");
                });
            }

            entry.setStatus("EXPIRED");
            waitlistRepository.save(entry);

            // ── THE CASCADE ────────────────────────────────────────────────
            // The line must keep moving. If person #1's offer expires
            // unclaimed, person #2 should be offered immediately — not
            // wait for the next refund. Without this line, an ignored
            // offer would silently stall the entire waitlist for this event.
            waitlistService.offerNextInLine(entry.getEventId());
        }
    }
}