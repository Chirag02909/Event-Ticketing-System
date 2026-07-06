package com.ticketing.service.interfaces;

import com.ticketing.model.SeatVO;

/**
 * Publishes seat status changes to WebSocket subscribers.
 * Called from ReservationServiceImpl, PaymentServiceImpl, and
 * SeatHoldExpiryScheduler — anywhere a seat's status changes.
 */
public interface SeatBroadcastService {

    /**
     * Broadcasts a single seat's updated status to all clients
     * subscribed to /topic/event/{eventId}/seats.
     *
     * @param seat      the seat whose status just changed
     * @param eventType SEAT_HELD / SEAT_RELEASED / SEAT_BOOKED
     */
    void broadcastSeatUpdate(SeatVO seat, String eventType);
}