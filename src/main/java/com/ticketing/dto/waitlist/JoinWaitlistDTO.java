package com.ticketing.dto.waitlist;

import jakarta.validation.constraints.NotNull;

public class JoinWaitlistDTO {

    @NotNull(message = "Event ID is required")
    private Long eventId;

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
}