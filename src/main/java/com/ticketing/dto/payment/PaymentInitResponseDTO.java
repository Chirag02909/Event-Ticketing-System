package com.ticketing.dto.payment;

import java.math.BigDecimal;

/**
 * Returned to the frontend after your backend creates a Razorpay order.
 *
 * The frontend uses razorpayOrderId + razorpayKeyId to initialise
 * the Razorpay checkout popup:
 *
 *   const options = {
 *     key: data.razorpayKeyId,
 *     amount: data.amountInPaise,
 *     currency: "INR",
 *     order_id: data.razorpayOrderId,   ← this is what links payment to your order
 *     ...
 *   };
 *   const rzp = new Razorpay(options);
 *   rzp.open();
 */
public class PaymentInitResponseDTO {

    private String razorpayOrderId;
    private String razorpayKeyId;       // your Razorpay public key (safe to expose)
    private long amountInPaise;         // Razorpay uses paise (₹500 = 50000 paise)
    private String currency;
    private String bookingRef;
    private BigDecimal displayAmount;   // ₹500.00 — for showing in the UI

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }

    public String getRazorpayKeyId() { return razorpayKeyId; }
    public void setRazorpayKeyId(String razorpayKeyId) { this.razorpayKeyId = razorpayKeyId; }

    public long getAmountInPaise() { return amountInPaise; }
    public void setAmountInPaise(long amountInPaise) { this.amountInPaise = amountInPaise; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getBookingRef() { return bookingRef; }
    public void setBookingRef(String bookingRef) { this.bookingRef = bookingRef; }

    public BigDecimal getDisplayAmount() { return displayAmount; }
    public void setDisplayAmount(BigDecimal displayAmount) { this.displayAmount = displayAmount; }
}