package com.ticketing.service.interfaces;

import com.ticketing.dto.Response;
import org.springframework.http.ResponseEntity;

public interface WebhookService {

    /**
     * Entry point for all Razorpay webhook events.
     *
     * Responsibilities:
     *   1. Verify HMAC-SHA256 signature against raw request body
     *   2. Parse event type from JSON payload
     *   3. Route to the correct handler (payment.captured / payment.failed)
     *   4. Return 200 quickly — Razorpay marks webhooks as failed if no
     *      response within 5 seconds
     *
     * @param rawBody   raw request body string (must NOT be pre-parsed)
     * @param signature value of X-Razorpay-Signature header
     */
    ResponseEntity<Response> handleWebhook(String rawBody, String signature);
}