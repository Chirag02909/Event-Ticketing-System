package com.ticketing.service.interfaces;

import com.ticketing.dto.refund.RefundResponseDTO;
import com.ticketing.dto.refund.RequestRefundDTO;
import org.springframework.http.ResponseEntity;

public interface RefundService {

    /**
     * Initiates a refund for a CONFIRMED booking.
     * Validates ownership, booking status, and event timing, then
     * calls Razorpay's refund API and flips the booking to REFUND_PENDING.
     *
     * @param bookingId the booking to refund
     * @param dto       optional reason for the refund
     * @param userEmail the requesting user's email (must own the booking)
     */
    ResponseEntity<RefundResponseDTO> requestRefund(
        Long bookingId, RequestRefundDTO dto, String userEmail
    );

    /**
     * Called by WebhookServiceImpl after signature verification, when a
     * refund.processed event arrives. Fully idempotent — safe to call
     * multiple times with the same razorpayRefundId.
     *
     * @param razorpayRefundId from the webhook payload
     * @param status           "processed" or "failed" from Razorpay
     */
    ResponseEntity<com.ticketing.dto.Response> confirmRefund(
        String razorpayRefundId, String status
    );

    /**
     * Fetches the refund status for a booking, if one exists.
     */
    ResponseEntity<RefundResponseDTO> getRefundStatus(Long bookingId, String userEmail);
}