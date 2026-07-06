package com.ticketing.dto.refund;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RefundResponseDTO {

    private boolean success;
    private String message;
    private Long bookingId;
    private String bookingRef;
    private String refundStatus;     // PENDING / SUCCESS / FAILED
    private BigDecimal amount;
    private LocalDateTime requestedAt;

    public RefundResponseDTO() {}

    public RefundResponseDTO(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getBookingRef() { return bookingRef; }
    public void setBookingRef(String bookingRef) { this.bookingRef = bookingRef; }

    public String getRefundStatus() { return refundStatus; }
    public void setRefundStatus(String refundStatus) { this.refundStatus = refundStatus; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
}