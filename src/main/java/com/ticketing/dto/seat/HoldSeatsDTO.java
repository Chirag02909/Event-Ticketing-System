package com.ticketing.dto.seat;

import jakarta.validation.constraints.*;
import java.util.List;

/**
 * Sent by the user when they click seats on the seat map and hit "Hold Seats".
 * The service will attempt to hold all requested seats atomically.
 * If ANY seat is already HELD or BOOKED, the entire request is rejected.
 */
public class HoldSeatsDTO {

    @NotNull(message = "Seat IDs are required")
    @Size(min = 1, max = 10, message = "You can hold between 1 and 10 seats at a time")
    private List<Long> seatIds;

    @NotNull(message = "Event ID is required")
    private Long eventId;

    public List<Long> getSeatIds() { return seatIds; }
    public void setSeatIds(List<Long> seatIds) { this.seatIds = seatIds; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
}