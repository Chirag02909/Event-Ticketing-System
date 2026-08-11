package com.ticketing.service.impl;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import com.ticketing.dto.Response;
import com.ticketing.dto.refund.RefundResponseDTO;
import com.ticketing.dto.refund.RequestRefundDTO;
import com.ticketing.model.*;
import com.ticketing.repository.*;
import com.ticketing.service.interfaces.EmailService;
import com.ticketing.service.interfaces.RefundService;
import com.ticketing.service.interfaces.SeatBroadcastService;
import com.ticketing.service.interfaces.WaitlistService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RefundServiceImpl implements RefundService {

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Autowired private BookingRepository     bookingRepository;
    @Autowired private PaymentRepository     paymentRepository;
    @Autowired private RefundRepository      refundRepository;
    @Autowired private SeatRepository        seatRepository;
    @Autowired private BookingSeatRepository bookingSeatRepository;
    @Autowired private UserDAO        userRepository;
    @Autowired private EventRepository       eventRepository;
    @Autowired private SeatBroadcastService  seatBroadcastService;
    @Autowired private EmailService          emailService;
    @Autowired private WaitlistService waitlistService;

    // ─────────────────────────────────────────────────────────────────────
    // REQUEST REFUND
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<RefundResponseDTO> requestRefund(
            Long bookingId, RequestRefundDTO dto, String userEmail) {

        // ── Resolve user ──────────────────────────────────────────────────
        Optional<UserVO> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        UserVO user = userOpt.get();

        // ── Load and validate booking ─────────────────────────────────────
        Optional<BookingVO> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new RefundResponseDTO(false, "Booking not found."));
        }

        BookingVO booking = bookingOpt.get();

        if (!booking.getUserId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new RefundResponseDTO(false, "You do not own this booking."));
        }

        // Only CONFIRMED bookings can be refunded — this is what
        // distinguishes a refund from a plain cancellation (Chunk 4
        // already handles cancelling unpaid PENDING_PAYMENT bookings).
        if (!"CONFIRMED".equals(booking.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RefundResponseDTO(false,
                    "Only confirmed bookings can be refunded. Current status: "
                    + booking.getStatus()));
        }

        // Block refunds after the event has already happened
        Optional<EventVO> eventOpt = eventRepository.findById(booking.getEventId());
        if (eventOpt.isPresent() && eventOpt.get().getEventDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RefundResponseDTO(false,
                    "This event has already taken place. Refunds are not available."));
        }

        // Prevent duplicate refund requests on the same booking
        if (refundRepository.findByBookingId(booking.getId()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new RefundResponseDTO(false,
                    "A refund has already been requested for this booking."));
        }

        // ── Find the original successful payment ─────────────────────────
        Optional<PaymentVO> paymentOpt = paymentRepository.findByBookingId(booking.getId());
        if (paymentOpt.isEmpty() || !"SUCCESS".equals(paymentOpt.get().getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RefundResponseDTO(false,
                    "No successful payment found for this booking."));
        }

        PaymentVO payment = paymentOpt.get();

        // ── Flip booking to REFUND_PENDING BEFORE calling Razorpay ────────
        // This prevents a second refund request from racing in while the
        // Razorpay API call is in flight — same defensive pattern as
        // holding seats before confirming a booking in Chunk 4.
        booking.setStatus("REFUND_PENDING");
        bookingRepository.save(booking);

        // ── Call Razorpay's Refund API ────────────────────────────────────
        long amountInPaise = payment.getAmount()
            .multiply(BigDecimal.valueOf(100))
            .longValue();

        try {
            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", amountInPaise);
            refundRequest.put("speed", "normal");
            if (dto.getReason() != null && !dto.getReason().isBlank()) {
                JSONObject notes = new JSONObject();
                notes.put("reason", dto.getReason());
                refundRequest.put("notes", notes);
            }

            System.out.println("========== RAZORPAY REFUND DEBUG ==========");
            System.out.println("Razorpay Payment ID: " + payment.getRazorpayPaymentId());
            System.out.println("Database Amount: " + payment.getAmount());
            System.out.println("Refund Amount Paise: " + amountInPaise);
            System.out.println("===========================================");

            Refund refund = razorpayClient.payments.refund(
                payment.getRazorpayPaymentId(), refundRequest
            );
            String razorpayRefundId = refund.get("id");

            // ── Save RefundVO ──────────────────────────────────────────────
            RefundVO refundRecord = new RefundVO();
            refundRecord.setBookingId(booking.getId());
            refundRecord.setPaymentId(payment.getId());
            refundRecord.setRazorpayRefundId(razorpayRefundId);
            refundRecord.setAmount(payment.getAmount());
            refundRecord.setStatus("PENDING");
            refundRepository.save(refundRecord);

            RefundResponseDTO response = new RefundResponseDTO(true,
                "Refund initiated. You will be notified once it completes.");
            response.setBookingId(booking.getId());
            response.setBookingRef(booking.getBookingRef());
            response.setRefundStatus("PENDING");
            response.setAmount(payment.getAmount());
            response.setRequestedAt(refundRecord.getRequestedAt());

            return ResponseEntity.ok(response);

        } catch (RazorpayException e) {
            // Roll the booking back to CONFIRMED — the refund never
            // actually started on Razorpay's side, so the booking is
            // still valid and the user can retry.
            booking.setStatus("CONFIRMED");
            bookingRepository.save(booking);

            System.out.println("[RefundService] Razorpay refund API call failed: "
                + e.getMessage());

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new RefundResponseDTO(false,
                    "Refund could not be initiated. Please try again shortly."));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // CONFIRM REFUND — webhook calls this
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<Response> confirmRefund(
            String razorpayRefundId, String status) {

        Response response = new Response();

        // ── IDEMPOTENCY CHECK ──────────────────────────────────────────────
        // Identical pattern to PaymentServiceImpl.confirmPayment() — if this
        // exact refund has already been finalised, do nothing and return 200
        // so Razorpay stops retrying.
        Optional<RefundVO> refundOpt = refundRepository.findByRazorpayRefundId(razorpayRefundId);

        if (refundOpt.isEmpty()) {

            response.setStatus(false);
            response.setMessage("No refund record found for this ID.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        RefundVO refund = refundOpt.get();

        if (!"PENDING".equals(refund.getStatus())) {
            // Already processed (SUCCESS or FAILED) — idempotent no-op

            response.setStatus(true);
            response.setMessage("Refund already in a final state. Idempotent response.");
            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        Optional<BookingVO> bookingOpt = bookingRepository.findById(refund.getBookingId());
        if (bookingOpt.isEmpty()) {

            response.setStatus(false);
            response.setMessage("Booking not found for this refund.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        BookingVO booking = bookingOpt.get();

        if ("processed".equalsIgnoreCase(status)) {
            handleRefundSuccess(refund, booking);
        } else {
            handleRefundFailure(refund, booking);
        }

        response.setStatus(true);
        response.setMessage("Refund webhook processed successfully.");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private void handleRefundSuccess(RefundVO refund, BookingVO booking) {
        refund.setStatus("SUCCESS");
        refund.setProcessedAt(LocalDateTime.now());
        refundRepository.save(refund);

        booking.setStatus("REFUNDED");
        bookingRepository.save(booking);

        // ── Release seats: BOOKED → AVAILABLE ─────────────────────────────
        List<BookingSeatVO> seatLinks = bookingSeatRepository.findByBookingId(booking.getId());

        for (BookingSeatVO link : seatLinks) {
            seatRepository.findById(link.getSeatId()).ifPresent(seat -> {
                seat.setStatus("AVAILABLE");
                seat.setHeldBy(null);
                seat.setHeldUntil(null);
                seatRepository.save(seat);

                // Reuses the exact WebSocket broadcast infra from Chunk 6
                seatBroadcastService.broadcastSeatUpdate(seat, "SEAT_RELEASED");
            });
        }

        // ── NEW IN CHUNK 9: cascade to the waitlist ────────────────────────
        // A refund just freed up a seat for this event. If anyone is
        // waiting in line, offer it to them immediately rather than
        // leaving it sitting AVAILABLE until someone happens to browse in.
        // offerNextInLine() is a no-op (returns false) if the waitlist
        // for this event is empty, so this is always safe to call.
        waitlistService.offerNextInLine(booking.getEventId());

        // ── Async confirmation email — reuses Chunk 7's @Async pattern ───
        userRepository.findById(booking.getUserId()).ifPresent(user ->
            eventRepository.findById(booking.getEventId()).ifPresent(event ->
                emailService.sendBookingConfirmation(
                    user.getEmail(),
                    booking.getBookingRef() + " (Refunded)",
                    event.getTitle(),
                    List.of("Refund of ₹" + refund.getAmount() + " has been processed.")
                )
            )
        );
    }

    private void handleRefundFailure(RefundVO refund, BookingVO booking) {
        refund.setStatus("FAILED");
        refund.setFailureReason("Razorpay reported refund failure.");
        refund.setProcessedAt(LocalDateTime.now());
        refundRepository.save(refund);

        // Booking goes back to CONFIRMED — the refund didn't go through,
        // so as far as the user is concerned, their booking still stands.
        booking.setStatus("CONFIRMED");
        bookingRepository.save(booking);
    }

    // ─────────────────────────────────────────────────────────────────────
    // GET REFUND STATUS
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<RefundResponseDTO> getRefundStatus(Long bookingId, String userEmail) {
        Optional<UserVO> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Optional<BookingVO> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        BookingVO booking = bookingOpt.get();
        if (!booking.getUserId().equals(userOpt.get().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<RefundVO> refundOpt = refundRepository.findByBookingId(bookingId);
        if (refundOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new RefundResponseDTO(false, "No refund found for this booking."));
        }

        RefundVO refund = refundOpt.get();
        RefundResponseDTO response = new RefundResponseDTO(true, "Refund status retrieved.");
        response.setBookingId(booking.getId());
        response.setBookingRef(booking.getBookingRef());
        response.setRefundStatus(refund.getStatus());
        response.setAmount(refund.getAmount());
        response.setRequestedAt(refund.getRequestedAt());

        return ResponseEntity.ok(response);
    }
}