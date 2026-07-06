package com.ticketing.service.interfaces;

import com.ticketing.dto.Response;
import com.ticketing.dto.waitlist.JoinWaitlistDTO;
import com.ticketing.dto.waitlist.WaitlistResponseDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface WaitlistService {

    /**
     * Joins the waitlist for a sold-out event. Rejects if the event
     * still has available seats (the user should book directly instead)
     * or if the user already has an active entry for this event.
     */
    ResponseEntity<WaitlistResponseDTO> joinWaitlist(JoinWaitlistDTO dto, String userEmail);

    /**
     * Voluntarily leaves the waitlist before being offered a seat.
     */
    ResponseEntity<Response> leaveWaitlist(Long entryId, String userEmail);

    /**
     * Checks the current status and position-in-line for a waitlist entry.
     */
    ResponseEntity<WaitlistResponseDTO> getStatus(Long entryId, String userEmail);

    /**
     * Lists all of a user's waitlist entries across all events.
     */
    ResponseEntity<List<WaitlistResponseDTO>> getMyWaitlistEntries(String userEmail);

    /**
     * THE CORE TRIGGER: attempts to offer the next available seat to the
     * next person in line for an event. Called from two places:
     *   1. RefundServiceImpl, immediately after a refund releases a seat
     *   2. WaitlistExpiryScheduler, after an unclaimed offer expires and
     *      cascades to the next person
     *
     * Internally uses WaitlistRepository.claimNextInLine() for the
     * race-free FIFO claim, then assigns a specific AVAILABLE seat to
     * the claimed entry and holds it via the existing SeatVO mechanism.
     *
     * @param eventId the event that just had a seat free up
     * @return true if an offer was successfully made, false if the
     *         waitlist was empty (nothing to do)
     */
    boolean offerNextInLine(Long eventId);

    /**
     * User accepts an OFFERED waitlist entry — converts it into a normal
     * seat hold using the existing Chunk 4 hold→pay flow.
     */
    ResponseEntity<com.ticketing.dto.booking.BookingResponseDTO> acceptOffer(
        Long entryId, String userEmail
    );
}