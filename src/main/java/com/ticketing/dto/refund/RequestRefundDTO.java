package com.ticketing.dto.refund;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/bookings/{id}/refund.
 * reason is optional but recommended for support/audit purposes.
 */
public class RequestRefundDTO {

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}