package com.ticketing.dto.websocket;

import java.math.BigDecimal;

/**
 * The message payload broadcast over WebSocket whenever a seat's
 * status changes. Sent to /topic/event/{eventId}/seats.
 *
 * Frontend receives this and updates exactly one seat's colour on the
 * seat map — it does not need to re-fetch the entire seat list.
 *
 * eventType distinguishes WHY the seat changed, so the frontend can
 * show different feedback (e.g. a brief flash animation for HELD,
 * a confirmation toast for BOOKED).
 */
public class SeatUpdateMessage {

    private Long seatId;
    private String seatNumber;
    private String status;        // AVAILABLE / HELD / BOOKED
    private String eventType;     // SEAT_HELD / SEAT_RELEASED / SEAT_BOOKED
    private Long eventId;

    public SeatUpdateMessage() {}

    public SeatUpdateMessage(Long seatId, String seatNumber, String status,
                              String eventType, Long eventId) {
        this.seatId = seatId;
        this.seatNumber = seatNumber;
        this.status = status;
        this.eventType = eventType;
        this.eventId = eventId;
    }

    public Long getSeatId() { return seatId; }
    public void setSeatId(Long seatId) { this.seatId = seatId; }

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
}