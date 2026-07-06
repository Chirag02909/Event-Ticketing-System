package com.ticketing.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records a refund attempt for a CONFIRMED booking.
 *
 * WHY THIS IS A SEPARATE TABLE FROM PaymentVO:
 * A payment and its refund are two distinct financial events with
 * independent lifecycles. Bolting refund fields onto PaymentVO would
 * conflate "was this booking paid for" with "was it later reversed" —
 * two different questions that an auditor, a support agent, or a
 * future partial-refund feature would need to answer separately.
 *
 * ── IDEMPOTENCY (mirrors PaymentVO exactly) ─────────────────────────────
 * Razorpay's refund.processed webhook can fire more than once for the
 * same refund_id (network retries, timeouts). Before processing, we
 * check: does a RefundVO already exist with this razorpayRefundId AND
 * status = SUCCESS? If yes, return 200 immediately, do nothing.
 * The unique index makes that check an index lookup, not a table scan.
 * ────────────────────────────────────────────────────────────────────────
 */
@Entity
@Table(name = "refunds", indexes = {
    @Index(name = "idx_refunds_razorpay_refund_id",
           columnList = "razorpay_refund_id", unique = true)
})
public class RefundVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;

    /**
     * The original payment being refunded — kept for audit purposes
     * even though it's also derivable via bookingId.
     */
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    /**
     * ID returned by Razorpay when we call their Refund API.
     * Format: "rfnd_XxXxXxXxXxXxXx"
     */
    @Column(name = "razorpay_refund_id", length = 50)
    private String razorpayRefundId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * PENDING / SUCCESS / FAILED
     */
    @Column(name = "status", nullable = false, length = 15)
    private String status = "PENDING";

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        requestedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public String getRazorpayRefundId() { return razorpayRefundId; }
    public void setRazorpayRefundId(String razorpayRefundId) { this.razorpayRefundId = razorpayRefundId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}