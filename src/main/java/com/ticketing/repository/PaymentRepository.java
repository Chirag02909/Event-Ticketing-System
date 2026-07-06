package com.ticketing.repository;

import com.ticketing.model.PaymentVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentVO, Long> {

    Optional<PaymentVO> findByBookingId(Long bookingId);

    /**
     * Idempotency check — used by webhook handler before processing.
     * If a PaymentVO with this razorpayPaymentId and status=SUCCESS
     * already exists, the webhook has already been processed.
     * The unique index on razorpay_payment_id makes this an index lookup.
     */
    Optional<PaymentVO> findByRazorpayPaymentId(String razorpayPaymentId);

    /**
     * Find payment by Razorpay order ID — used to locate the payment
     * record when the webhook arrives (webhook carries payment ID,
     * we need to find the booking via order ID stored in PaymentVO).
     */
    Optional<PaymentVO> findByRazorpayOrderId(String razorpayOrderId);

    List<PaymentVO> findByStatus(String status);
}