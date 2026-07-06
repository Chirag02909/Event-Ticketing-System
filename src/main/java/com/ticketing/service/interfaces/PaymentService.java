package com.ticketing.service.interfaces;

import com.ticketing.dto.payment.CreateOrderDTO;
import com.ticketing.dto.payment.PaymentInitResponseDTO;
import com.ticketing.dto.Response;
import org.springframework.http.ResponseEntity;

public interface PaymentService {

    /**
     * Creates a Razorpay order for a PENDING_PAYMENT booking.
     * Returns the order ID and key needed by the frontend to open
     * the Razorpay checkout popup.
     */
    ResponseEntity<PaymentInitResponseDTO> createOrder(
        CreateOrderDTO dto, String userEmail
    );

    /**
     * Called by WebhookServiceImpl after signature verification passes.
     * Confirms the booking, flips seats to BOOKED, generates tickets.
     * Fully idempotent — safe to call multiple times with the same paymentId.
     *
     * @param razorpayOrderId   from webhook payload
     * @param razorpayPaymentId from webhook payload (unique per transaction)
     * @param razorpaySignature from X-Razorpay-Signature header
     */
    ResponseEntity<Response> confirmPayment(
        String razorpayOrderId,
        String razorpayPaymentId,
        String razorpaySignature
    );
}