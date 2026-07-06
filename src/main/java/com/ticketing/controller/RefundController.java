package com.ticketing.controller;

import com.ticketing.dto.refund.RefundResponseDTO;
import com.ticketing.dto.refund.RequestRefundDTO;
import com.ticketing.service.interfaces.RefundService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * Refund controller — JWT required, any authenticated user.
 * The webhook that CONFIRMS a refund lives in WebhookController
 * (updated below), not here — Razorpay calls that one directly.
 */
@RestController
@RequestMapping("/api/bookings")
public class RefundController {

    @Autowired
    private RefundService refundService;

    /**
     * Request a refund for a CONFIRMED booking.
     *
     * POST /api/bookings/{id}/refund
     * Body: { "reason": "Optional reason text" }
     */
    @PostMapping("/{id}/refund")
    public ResponseEntity<RefundResponseDTO> requestRefund(
            @PathVariable Long id,
            @Valid @RequestBody RequestRefundDTO dto,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                .body(new RefundResponseDTO(false,
                    bindingResult.getFieldErrors().get(0).getDefaultMessage()));
        }

        return refundService.requestRefund(id, dto, getEmail());
    }

    /**
     * Check the refund status for a booking.
     *
     * GET /api/bookings/{id}/refund
     */
    @GetMapping("/{id}/refund")
    public ResponseEntity<RefundResponseDTO> getRefundStatus(@PathVariable Long id) {
        return refundService.getRefundStatus(id, getEmail());
    }

    private String getEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}