package com.ticketing.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records a payment attempt for a booking.
 *
 * ONE PAYMENT PER BOOKING (1:1 enforced by unique constraint on booking_id).
 * If a payment fails and the user retries, we UPDATE the existing PaymentVO
 * rather than inserting a new one — this keeps auditing clean.
 *
 * ── IDEMPOTENCY (why razorpay_payment_id has a unique index) ────────────────
 * Razorpay's webhook may fire the same "payment.captured" event multiple times
 * due to network retries. Before processing a webhook, WebhookServiceImpl
 * checks: "Does a PaymentVO already exist with this razorpay_payment_id AND
 * status = SUCCESS?" If yes → return 200 immediately, do nothing.
 * This unique index makes that check a fast index lookup, not a table scan.
 * ────────────────────────────────────────────────────────────────────────────
 *
 * ── RAZORPAY ID GLOSSARY ────────────────────────────────────────────────────
 * razorpay_order_id   → created by YOUR backend when user initiates checkout
 *                       format: "order_XxXxXxXxXxXxXx"
 * razorpay_payment_id → created by RAZORPAY when payment succeeds
 *                       format: "pay_XxXxXxXxXxXxXx"
 *                       arrives in the webhook payload
 * ────────────────────────────────────────────────────────────────────────────
 */
@Entity
@Table(name = "payments", indexes = {
        // Fast idempotency check in webhook handler
        @Index(name = "idx_payments_razorpay_payment_id",
                columnList = "razorpay_payment_id", unique = true)
})
public class PaymentVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID returned by Razorpay when we create an order via their API.
     * Sent to the frontend to initialise the Razorpay checkout popup.
     */
    @Column(name = "razorpay_order_id", nullable = false, length = 50)
    private String razorpayOrderId;

    /**
     * ID returned by Razorpay in the webhook after payment succeeds.
     * Null until payment is captured. Unique index enables idempotency.
     */
    @Column(name = "razorpay_payment_id", length = 50)
    private String razorpayPaymentId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * PENDING / SUCCESS / FAILED / REFUNDED
     */
    @Column(name = "status", nullable = false, length = 15)
    private String status = "PENDING";

    /**
     * Populated if status = FAILED. Stores Razorpay's error description
     * so organisers and support can investigate payment failures.
     */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }

    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}