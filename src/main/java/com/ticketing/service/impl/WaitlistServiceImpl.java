package com.ticketing.service.impl;

import com.ticketing.dto.Response;
import com.ticketing.dto.booking.BookingResponseDTO;
import com.ticketing.dto.seat.HoldSeatsDTO;
import com.ticketing.dto.waitlist.JoinWaitlistDTO;
import com.ticketing.dto.waitlist.WaitlistResponseDTO;
import com.ticketing.model.*;
import com.ticketing.repository.*;
import com.ticketing.service.interfaces.ReservationService;
import com.ticketing.service.interfaces.SeatBroadcastService;
import com.ticketing.service.interfaces.EmailService;
import com.ticketing.service.interfaces.WaitlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class WaitlistServiceImpl implements WaitlistService {

    private static final List<String> ACTIVE_STATUSES = List.of("WAITING", "OFFERED");

    @Value("${ticketing.waitlist.offer.minutes:30}")
    private int offerMinutes;

    @Autowired private WaitlistRepository    waitlistRepository;
    @Autowired private EventRepository       eventRepository;
    @Autowired private SeatRepository        seatRepository;
    @Autowired private UserDAO        userRepository;
    @Autowired private ReservationService    reservationService;
    @Autowired private SeatBroadcastService  seatBroadcastService;
    @Autowired private EmailService          emailService;

    // ─────────────────────────────────────────────────────────────────────
    // JOIN WAITLIST
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<WaitlistResponseDTO> joinWaitlist(
            JoinWaitlistDTO dto, String userEmail) {

        Optional<UserVO> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        UserVO user = userOpt.get();

        Optional<EventVO> eventOpt = eventRepository.findById(dto.getEventId());
        if (eventOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new WaitlistResponseDTO(false, "Event not found."));
        }

        EventVO event = eventOpt.get();
        if (!"PUBLISHED".equals(event.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new WaitlistResponseDTO(false, "This event is not open for booking."));
        }

        // The waitlist only makes sense for sold-out events. If seats
        // are still available, the user should book directly instead —
        // joining the waitlist here would be a confusing dead end.
        long availableCount = seatRepository.countByEventIdAndStatus(event.getId(), "AVAILABLE");
        if (availableCount > 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new WaitlistResponseDTO(false,
                    "This event still has " + availableCount + " seat(s) available. " +
                    "Please book directly instead of joining the waitlist."));
        }

        // Prevent duplicate active entries for the same user + event
        Optional<WaitlistEntryVO> existing = waitlistRepository
            .findByUserIdAndEventIdAndStatusIn(user.getId(), event.getId(), ACTIVE_STATUSES);
        if (existing.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new WaitlistResponseDTO(false,
                    "You are already on the waitlist for this event."));
        }

        WaitlistEntryVO entry = new WaitlistEntryVO();
        entry.setEventId(event.getId());
        entry.setUserId(user.getId());
        entry.setStatus("WAITING");
        WaitlistEntryVO saved = waitlistRepository.save(entry);

        long position = waitlistRepository.countAheadInLine(event.getId(), saved.getId()) + 1;

        WaitlistResponseDTO response = new WaitlistResponseDTO(true,
            "You've joined the waitlist. We'll notify you if a seat opens up.");
        response.setEntryId(saved.getId());
        response.setEventId(event.getId());
        response.setStatus("WAITING");
        response.setPositionInLine(position);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─────────────────────────────────────────────────────────────────────
    // LEAVE WAITLIST
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<Response> leaveWaitlist(Long entryId, String userEmail) {

        Response response = new Response();

        Optional<UserVO> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {

            response.setStatus(false);
            response.setMessage("User not found.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        Optional<WaitlistEntryVO> entryOpt = waitlistRepository.findById(entryId);
        if (entryOpt.isEmpty()) {

            response.setStatus(false);
            response.setMessage("Waitlist entry not found.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        WaitlistEntryVO entry = entryOpt.get();
        if (!entry.getUserId().equals(userOpt.get().getId())) {

            response.setStatus(false);
            response.setMessage("This is not your waitlist entry.");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }

        if ("OFFERED".equals(entry.getStatus())) {
            // Releasing an offered seat needs to also free the held seat
            // and cascade to the next person — same as letting it expire,
            // just triggered immediately instead of waiting for the timer.
            releaseOfferedSeat(entry);
        }

        entry.setStatus("CANCELLED");
        waitlistRepository.save(entry);

        response.setStatus(true);
        response.setMessage("You have left the waitlist.");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // ─────────────────────────────────────────────────────────────────────
    // GET STATUS
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<WaitlistResponseDTO> getStatus(Long entryId, String userEmail) {
        Optional<UserVO> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Optional<WaitlistEntryVO> entryOpt = waitlistRepository.findById(entryId);
        if (entryOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        WaitlistEntryVO entry = entryOpt.get();
        if (!entry.getUserId().equals(userOpt.get().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(toDTO(entry));
    }

    @Override
    public ResponseEntity<List<WaitlistResponseDTO>> getMyWaitlistEntries(String userEmail) {
        Optional<UserVO> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Simple approach: fetch all, filter in service layer. For this
        // project's scale this is fine; a high-volume system would add
        // a dedicated findByUserId query instead.
        List<WaitlistResponseDTO> entries = waitlistRepository.findAll().stream()
            .filter(e -> e.getUserId().equals(userOpt.get().getId()))
            .map(this::toDTO)
            .collect(Collectors.toList());

        return ResponseEntity.ok(entries);
    }

    // ─────────────────────────────────────────────────────────────────────
    // OFFER NEXT IN LINE — the core trigger, called from RefundServiceImpl
    // and from the expiry scheduler
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public boolean offerNextInLine(Long eventId) {

        // ── Step 1: find an AVAILABLE seat to offer ───────────────────────
        // We need a real seat in hand before claiming a waitlist position —
        // no point claiming a line position if there's nothing to offer.
        List<SeatVO> availableSeats = seatRepository.findByEventId(eventId).stream()
            .filter(s -> "AVAILABLE".equals(s.getStatus()))
            .toList();

        if (availableSeats.isEmpty()) {
            return false; // nothing to offer
        }

        SeatVO seatToOffer = availableSeats.get(0);

        // ── Step 2: ATOMIC CLAIM — flip the oldest WAITING entry to OFFERED ─
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(offerMinutes);

        int rowsClaimed = waitlistRepository.claimNextInLine(eventId, now, expiresAt);

        if (rowsClaimed == 0) {
            return false; // waitlist was empty — nothing to do
        }

        // ── Step 3: find which entry was just claimed ─────────────────────
        List<WaitlistEntryVO> justClaimed =
            waitlistRepository.findRecentlyClaimedWithoutSeat(eventId);

        if (justClaimed.isEmpty()) {
            // Defensive guard — should not happen given rowsClaimed == 1,
            // but never silently swallow an inconsistent state.
            System.out.println("[WaitlistService] WARNING: claimNextInLine reported "
                + "1 row updated but no claimed entry found for event " + eventId);
            return false;
        }

        WaitlistEntryVO entry = justClaimed.get(0);

        // ── Step 4: hold the seat specifically for this user ──────────────
        seatToOffer.setStatus("HELD");
        seatToOffer.setHeldBy(entry.getUserId());
        seatToOffer.setHeldUntil(expiresAt);
        seatRepository.save(seatToOffer);

        entry.setOfferedSeatId(seatToOffer.getId());
        waitlistRepository.save(entry);

        // Reuses the exact WebSocket broadcast infra from Chunk 6
        seatBroadcastService.broadcastSeatUpdate(seatToOffer, "SEAT_HELD");

        // ── Step 5: notify the user ────────────────────────────────────────
        userRepository.findById(entry.getUserId()).ifPresent(user ->
            eventRepository.findById(eventId).ifPresent(event ->
                emailService.sendBookingConfirmation(
                    user.getEmail(),
                    "Waitlist offer",
                    event.getTitle(),
                    List.of("Seat " + seatToOffer.getSeatNumber() + " is available for you. "
                        + "You have " + offerMinutes + " minutes to claim it before it passes "
                        + "to the next person in line.")
                )
            )
        );

        return true;
    }

    // ─────────────────────────────────────────────────────────────────────
    // ACCEPT OFFER — converts an OFFERED entry into a real booking
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<BookingResponseDTO> acceptOffer(Long entryId, String userEmail) {
        Optional<UserVO> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Optional<WaitlistEntryVO> entryOpt = waitlistRepository.findById(entryId);
        if (entryOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        WaitlistEntryVO entry = entryOpt.get();

        if (!entry.getUserId().equals(userOpt.get().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!"OFFERED".equals(entry.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        if (entry.getOfferExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.GONE).build(); // offer expired
        }

        // Delegate to the EXISTING hold→pay flow from Chunk 4 — the seat
        // is already HELD for this user, so holdAndCreateBooking() will
        // find it AVAILABLE-to-this-user... actually it needs to already
        // be HELD by them, which it is. We re-use the booking creation
        // logic directly rather than re-implementing it here.
        HoldSeatsDTO holdDto = new HoldSeatsDTO();
        holdDto.setEventId(entry.getEventId());
        holdDto.setSeatIds(List.of(entry.getOfferedSeatId()));

        // Mark the waitlist entry as claimed regardless of downstream
        // payment outcome — the user has exercised their offer, and
        // normal booking cancellation rules apply from here on.
        entry.setStatus("CLAIMED");
        waitlistRepository.save(entry);

        ResponseEntity<BookingResponseDTO> result =
            reservationService.holdAndCreateBooking(holdDto, userEmail);

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────

    private void releaseOfferedSeat(WaitlistEntryVO entry) {
        if (entry.getOfferedSeatId() == null) return;

        seatRepository.findById(entry.getOfferedSeatId()).ifPresent(seat -> {
            seat.setStatus("AVAILABLE");
            seat.setHeldBy(null);
            seat.setHeldUntil(null);
            seatRepository.save(seat);
            seatBroadcastService.broadcastSeatUpdate(seat, "SEAT_RELEASED");
        });

        // Cascade to the next person in line — same seat, new offer
        offerNextInLine(entry.getEventId());
    }

    private WaitlistResponseDTO toDTO(WaitlistEntryVO entry) {
        WaitlistResponseDTO dto = new WaitlistResponseDTO(true, "OK");
        dto.setEntryId(entry.getId());
        dto.setEventId(entry.getEventId());
        dto.setStatus(entry.getStatus());

        if ("WAITING".equals(entry.getStatus())) {
            long position = waitlistRepository.countAheadInLine(entry.getEventId(), entry.getId()) + 1;
            dto.setPositionInLine(position);
        }

        if ("OFFERED".equals(entry.getStatus()) && entry.getOfferedSeatId() != null) {
            dto.setOfferedSeatId(entry.getOfferedSeatId());
            seatRepository.findById(entry.getOfferedSeatId())
                .ifPresent(seat -> dto.setOfferedSeatNumber(seat.getSeatNumber()));
            dto.setOfferExpiresAt(entry.getOfferExpiresAt());
        }

        return dto;
    }
}