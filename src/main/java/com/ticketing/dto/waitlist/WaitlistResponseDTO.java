package com.ticketing.dto.waitlist;

import java.time.LocalDateTime;

public class WaitlistResponseDTO {

    private boolean success;
    private String message;
    private Long entryId;
    private Long eventId;
    private String status;            // WAITING / OFFERED / CLAIMED / EXPIRED / CANCELLED
    private long positionInLine;      // 1-indexed; only meaningful when status = WAITING

    // Populated only when status = OFFERED
    private Long offeredSeatId;
    private String offeredSeatNumber;
    private LocalDateTime offerExpiresAt;

    public WaitlistResponseDTO() {}

    public WaitlistResponseDTO(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getEntryId() { return entryId; }
    public void setEntryId(Long entryId) { this.entryId = entryId; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getPositionInLine() { return positionInLine; }
    public void setPositionInLine(long positionInLine) { this.positionInLine = positionInLine; }

    public Long getOfferedSeatId() { return offeredSeatId; }
    public void setOfferedSeatId(Long offeredSeatId) { this.offeredSeatId = offeredSeatId; }

    public String getOfferedSeatNumber() { return offeredSeatNumber; }
    public void setOfferedSeatNumber(String offeredSeatNumber) { this.offeredSeatNumber = offeredSeatNumber; }

    public LocalDateTime getOfferExpiresAt() { return offerExpiresAt; }
    public void setOfferExpiresAt(LocalDateTime offerExpiresAt) { this.offerExpiresAt = offerExpiresAt; }
}