package com.ticketing.repository;

import com.ticketing.model.RefundVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<RefundVO, Long> {

    Optional<RefundVO> findByBookingId(Long bookingId);

    /**
     * Idempotency check — used by the refund webhook handler before
     * processing. Mirrors PaymentRepository.findByRazorpayPaymentId().
     */
    Optional<RefundVO> findByRazorpayRefundId(String razorpayRefundId);
}