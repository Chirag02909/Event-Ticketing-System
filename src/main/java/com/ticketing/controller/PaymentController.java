package com.ticketing.controller;

import com.ticketing.dto.payment.CreateOrderDTO;
import com.ticketing.dto.payment.PaymentInitResponseDTO;
import com.ticketing.service.interfaces.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * Payment controller — JWT required.
 *
 * ENDPOINT:
 *   POST /api/payments/create-order
 *
 * The webhook endpoint lives in WebhookController — it is PUBLIC
 * (no JWT) because Razorpay calls it server-to-server.
 * Security is provided by HMAC signature verification instead.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * Creates a Razorpay order for a PENDING_PAYMENT booking.
     *
     * POST /api/payments/create-order
     * Authorization: Bearer <jwt>
     * Body: { "bookingId": 42 }
     *
     * Returns: { razorpayOrderId, razorpayKeyId, amountInPaise, currency, bookingRef }
     *
     * Frontend uses the response to open Razorpay checkout popup:
     *   const rzp = new Razorpay({
     *     key: data.razorpayKeyId,
     *     amount: data.amountInPaise,
     *     order_id: data.razorpayOrderId,
     *     ...
     *   });
     *   rzp.open();
     */
    @PostMapping("/create-order")
    public ResponseEntity<PaymentInitResponseDTO> createOrder(
            @Valid @RequestBody CreateOrderDTO dto,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().build();
        }

        User user = (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String userEmail = user.getUsername();

        return paymentService.createOrder(dto, userEmail);
    }
}