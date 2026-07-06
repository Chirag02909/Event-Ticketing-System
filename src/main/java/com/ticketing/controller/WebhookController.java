package com.ticketing.controller;

import com.ticketing.dto.Response;
import com.ticketing.service.interfaces.WebhookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Webhook controller — PUBLIC endpoint (no JWT).
 *
 * WHY NO JWT:
 * Razorpay is a server calling your server. It has no user JWT.
 * Security is provided by HMAC-SHA256 signature verification
 * inside WebhookServiceImpl instead.
 *
 * CRITICAL — @RequestBody String rawBody:
 * The body MUST be read as a raw String, not parsed into a Java object.
 * HMAC verification requires the exact bytes Razorpay sent.
 * If Spring parses the body first (e.g. into a Map or JsonNode),
 * re-serializing it changes byte ordering and the signature never matches.
 *
 * SecurityConfig must explicitly permit /api/webhooks/** without JWT.
 */
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    @Autowired
    private WebhookService webhookService;

    /**
     * Receives all Razorpay webhook events.
     *
     * POST /api/webhooks/razorpay
     *
     * Razorpay expects a 200 response within 5 seconds.
     * If it receives anything else (or times out), it retries.
     * This is why idempotency in WebhookServiceImpl is critical.
     */
    @PostMapping("/razorpay")
    public ResponseEntity<Response> razorpayWebhook(
            @RequestBody String rawBody,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        System.out.println("WEBHOOK HIT");
        System.out.println("SIGNATURE = " + signature);
        System.out.println("BODY = " + rawBody);

        return webhookService.handleWebhook(rawBody, signature);
    }
}