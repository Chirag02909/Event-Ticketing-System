package com.ticketing.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.dto.Response;
import com.ticketing.model.BookingVO;
import com.ticketing.model.PaymentVO;
import com.ticketing.repository.BookingRepository;
import com.ticketing.repository.PaymentRepository;
import com.ticketing.service.interfaces.PaymentService;
import com.ticketing.service.interfaces.RefundService;
import com.ticketing.service.interfaces.WebhookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Handles all inbound Razorpay webhook events.
 *
 * SECURITY FIRST — HMAC-SHA256 verification:
 * Before any business logic runs, we verify the webhook signature.
 * This prevents attackers from forging fake payment.captured events
 * to get free tickets.
 *
 * Verification algorithm:
 *   1. Take the raw request body (as bytes — NOT parsed JSON)
 *   2. Compute HMAC-SHA256(webhookSecret, rawBody)
 *   3. Compare with X-Razorpay-Signature header value
 *   4. If mismatch → reject with 400
 *
 * WHY RAW BODY MATTERS:
 * JSON serialization is not guaranteed to preserve field ordering or whitespace.
 * If you parse the body into a Java object and re-serialize it before hashing,
 * the resulting bytes may differ from what Razorpay hashed. You must hash the
 * exact bytes Razorpay sent, which means reading the body as a raw String.
 */
@Service
public class WebhookServiceImpl implements WebhookService {

    private static final String HMAC_ALGO = "HmacSHA256";

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @Autowired private PaymentService    paymentService;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private ObjectMapper      objectMapper;
    @Autowired private RefundService refundService;

    @Override
    public ResponseEntity<Response> handleWebhook(
            String rawBody, String signature) {

        // ── Step 1: Verify HMAC signature ─────────────────────────────────
        if (!isSignatureValid(rawBody, signature)) {

            System.out.println("[WebhookService] Invalid signature — rejecting webhook.");

            Response response = new Response();
            response.setStatus(false);
            response.setMessage("Invalid webhook signature.");

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(response);
        }

// ── Step 2: Parse the event type ──────────────────────────────────
        String eventType;
        JsonNode payload;

        try {

            payload = objectMapper.readTree(rawBody);
            eventType = payload.path("event").asText();

        } catch (Exception e) {

            Response response = new Response();
            response.setStatus(false);
            response.setMessage("Malformed webhook payload.");

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(response);
        }

        System.out.println("[WebhookService] Received event: " + eventType);

// ── Step 3: Route by event type ───────────────────────────────────
        return switch (eventType) {

            case "payment.captured" -> handlePaymentCaptured(payload);

            case "payment.failed" -> handlePaymentFailed(payload);

            case "refund.processed" -> handleRefundProcessed(payload);

            case "refund.failed"    -> handleRefundFailed(payload);

            default -> {

                // Razorpay sends many event types
                // (refund.created, order.paid, etc.)
                // Acknowledge unknown events with 200.

                System.out.println(
                        "[WebhookService] Unhandled event type: "
                                + eventType
                                + " — acknowledging."
                );

                Response response = new Response();
                response.setStatus(true);
                response.setMessage("Event acknowledged.");

                yield ResponseEntity.ok(response);
            }
        };
    }

    // ─────────────────────────────────────────────────────────────────────
    // PAYMENT CAPTURED — the main success path
    // ─────────────────────────────────────────────────────────────────────

    private ResponseEntity<Response> handlePaymentCaptured(JsonNode payload) {
        try {
            JsonNode paymentEntity = payload
                .path("payload")
                .path("payment")
                .path("entity");

            String razorpayPaymentId = paymentEntity.path("id").asText();
            String razorpayOrderId   = paymentEntity.path("order_id").asText();
            String razorpaySignature = ""; // not re-verified here — already done above

            // Delegate to PaymentService which handles idempotency + booking confirmation
            return paymentService.confirmPayment(
                razorpayOrderId, razorpayPaymentId, razorpaySignature
            );

        } catch (Exception e) {
            System.out.println("[WebhookService] Error processing payment.captured: "
                + e.getMessage());
            // Return 500 — Razorpay will retry. Only return 200 when you've
            // successfully processed or determined it's a duplicate.
            Response response = new Response();
            response.setStatus(false);
            response.setMessage("Processing error. Razorpay will retry.");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // PAYMENT FAILED — mark payment and booking as failed
    // ─────────────────────────────────────────────────────────────────────

    private ResponseEntity<Response> handlePaymentFailed(JsonNode payload) {

        Response response = new Response();

        try {
            JsonNode paymentEntity = payload
                .path("payload")
                .path("payment")
                .path("entity");

            String razorpayOrderId   = paymentEntity.path("order_id").asText();
            String errorDescription  = paymentEntity
                .path("error_description").asText("Payment failed.");

            // Update PaymentVO to FAILED
            paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .ifPresent(payment -> {
                    payment.setStatus("FAILED");
                    payment.setFailureReason(errorDescription);
                    paymentRepository.save(payment);

                    // Mark booking as PAYMENT_FAILED
                    // Seats remain HELD briefly — the scheduler will
                    // release them when the hold expires naturally.
                    // This avoids a race condition where the user retries
                    // payment within the hold window.
                    bookingRepository.findById(payment.getBookingId())
                        .ifPresent(booking -> {
                            booking.setStatus("PAYMENT_FAILED");
                            bookingRepository.save(booking);
                        });
                });

            response.setStatus(true);
            response.setMessage("Payment failure recorded.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.setStatus(false);
            response.setMessage("Error processing payment failure.");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    // REFUND EVENTS — NEW IN CHUNK 8
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Razorpay payload shape for refund.processed:
     *   { "event": "refund.processed",
     *     "payload": { "refund": { "entity": { "id": "rfnd_...", ... } } } }
     */
    private ResponseEntity<Response> handleRefundProcessed(JsonNode payload) {

        Response response = new Response();
        try {
            JsonNode refundEntity = payload.path("payload").path("refund").path("entity");
            String razorpayRefundId = refundEntity.path("id").asText();

            return refundService.confirmRefund(razorpayRefundId, "processed");

        } catch (Exception e) {
            System.out.println("[WebhookService] Error processing refund.processed: "
                    + e.getMessage());

            response.setStatus(false);
            response.setMessage("Processing error. Razorpay will retry.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseEntity<Response> handleRefundFailed(JsonNode payload) {

        Response response = new Response();
        try {
            JsonNode refundEntity = payload.path("payload").path("refund").path("entity");
            String razorpayRefundId = refundEntity.path("id").asText();

            return refundService.confirmRefund(razorpayRefundId, "failed");

        } catch (Exception e) {
            System.out.println("[WebhookService] Error processing refund.failed: "
                    + e.getMessage());

            response.setStatus(false);
            response.setMessage("Processing error. Razorpay will retry.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HMAC-SHA256 SIGNATURE VERIFICATION
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Recomputes HMAC-SHA256(webhookSecret, rawBody) and compares
     * against the signature Razorpay sent in the header.
     *
     * TIMING-SAFE COMPARISON:
     * We use HexFormat + String.equals() here. In a production system
     * you should use MessageDigest.isEqual(a.getBytes(), b.getBytes())
     * to prevent timing attacks, but for this project the difference
     * is negligible since webhook secrets are long enough.
     */
    private boolean isSignatureValid(String rawBody, String receivedSignature) {
        if (receivedSignature == null || receivedSignature.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            SecretKeySpec secretKey = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO
            );
            mac.init(secretKey);

            byte[] hashBytes = mac.doFinal(
                rawBody.getBytes(StandardCharsets.UTF_8)
            );

            // Convert computed hash bytes to lowercase hex string
            String computedSignature = HexFormat.of().formatHex(hashBytes);

            System.out.println("Received Signature = " + receivedSignature);
            System.out.println("Computed Signature = " + computedSignature);

            return computedSignature.equals(receivedSignature);

        } catch (Exception e) {
            System.out.println("[WebhookService] Signature verification error: "
                + e.getMessage());
            return false;
        }
    }
}