package com.ticketing.service.impl;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.ticketing.dto.payment.CreateOrderDTO;
import com.ticketing.dto.payment.PaymentInitResponseDTO;
import com.ticketing.dto.Response;
import com.ticketing.model.*;
import com.ticketing.repository.*;
import com.ticketing.service.interfaces.EmailService;
import com.ticketing.service.interfaces.PaymentService;
import com.ticketing.service.interfaces.SeatBroadcastService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Autowired private BookingRepository     bookingRepository;
    @Autowired private PaymentRepository     paymentRepository;
    @Autowired private SeatRepository        seatRepository;
    @Autowired private BookingSeatRepository bookingSeatRepository;
    @Autowired private TicketRepository      ticketRepository;
    @Autowired private UserDAO        userRepository;
    @Autowired private EventRepository       eventRepository;

    // NEW — broadcasts seat status changes to WebSocket subscribers
    @Autowired private SeatBroadcastService seatBroadcastService;

    // NEW — sends the async confirmation email
    @Autowired private EmailService emailService;

    // ─────────────────────────────────────────────────────────────────────
    // CREATE RAZORPAY ORDER
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<PaymentInitResponseDTO> createOrder(
            CreateOrderDTO dto, String userEmail) {

        // ── Resolve user ──────────────────────────────────────────────────
        Optional<UserVO> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // ── Load and validate booking ─────────────────────────────────────
        Optional<BookingVO> bookingOpt = bookingRepository.findById(dto.getBookingId());
        if (bookingOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        BookingVO booking = bookingOpt.get();

        // Ownership check
        if (!booking.getUserId().equals(userOpt.get().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Only PENDING_PAYMENT bookings can be paid for
        if (!"PENDING_PAYMENT".equals(booking.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // Check if a Razorpay order already exists for this booking
        // (user clicked "Pay" twice — return the same order instead of creating a new one)
        Optional<PaymentVO> existingPayment = paymentRepository.findByBookingId(booking.getId());
        if (existingPayment.isPresent() && "PENDING".equals(existingPayment.get().getStatus())) {
            return ResponseEntity.ok(
                buildInitResponse(existingPayment.get(), booking.getTotalAmount())
            );
        }

        // ── Create Razorpay order ─────────────────────────────────────────
        // Razorpay uses paise (1 INR = 100 paise) — multiply by 100
        long amountInPaise = booking.getTotalAmount()
            .multiply(BigDecimal.valueOf(100))
            .longValue();

        try {
            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            // receipt ties this Razorpay order to our booking ref for support lookups
            orderRequest.put("receipt", booking.getBookingRef());

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            // ── Save PaymentVO ────────────────────────────────────────────
            PaymentVO payment = new PaymentVO();
            payment.setBookingId(booking.getId());
            payment.setRazorpayOrderId(razorpayOrderId);
            payment.setAmount(booking.getTotalAmount());
            payment.setStatus("PENDING");
            paymentRepository.save(payment);

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(buildInitResponse(payment, booking.getTotalAmount()));

        } catch (RazorpayException e) {
            System.out.println("[PaymentService] Razorpay order creation failed: "
                + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // CONFIRM PAYMENT — called by WebhookServiceImpl after signature check
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<Response> confirmPayment(
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature) {

        // ── IDEMPOTENCY CHECK — Step 1 ────────────────────────────────────
        // Has this exact payment already been processed successfully?
        // The unique index on razorpay_payment_id makes this a fast lookup.
        //
        // This is the guard against duplicate webhook delivery:
        //   First call:  payment not found → process → save with status=SUCCESS
        //   Second call: payment found with status=SUCCESS → return 200, do nothing
        // ─────────────────────────────────────────────────────────────────
        Optional<PaymentVO> existingByPaymentId =
                paymentRepository.findByRazorpayPaymentId(razorpayPaymentId);

        if (existingByPaymentId.isPresent()
                && "SUCCESS".equals(existingByPaymentId.get().getStatus())) {

            Response response = new Response();
            response.setStatus(true);
            response.setMessage("Payment already confirmed. Idempotent response.");

            return ResponseEntity.ok(response);
        }

// ── Locate the PaymentVO by order ID ─────────────────────────────
        Optional<PaymentVO> paymentOpt =
                paymentRepository.findByRazorpayOrderId(razorpayOrderId);

        if (paymentOpt.isEmpty()) {

            Response response = new Response();
            response.setStatus(false);
            response.setMessage("No payment record found for this order.");

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(response);
        }

        PaymentVO payment = paymentOpt.get();

// ── Load the booking ──────────────────────────────────────────────
        Optional<BookingVO> bookingOpt =
                bookingRepository.findByIdAndStatus(
                        payment.getBookingId(),
                        "PENDING_PAYMENT"
                );

        if (bookingOpt.isEmpty()) {

            Response response = new Response();
            response.setStatus(true);
            response.setMessage("Booking already in final state.");

            return ResponseEntity.ok(response);
        }

        BookingVO booking = bookingOpt.get();

// ── Update PaymentVO to SUCCESS ───────────────────────────────────
        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setStatus("SUCCESS");
        paymentRepository.save(payment);

// ── Confirm the booking ───────────────────────────────────────────
        booking.setStatus("CONFIRMED");
        bookingRepository.save(booking);

// ── Flip seats from HELD → BOOKED ────────────────────────────────
        List<BookingSeatVO> seatLinks =
                bookingSeatRepository.findByBookingId(booking.getId());

        for (BookingSeatVO link : seatLinks) {
            seatRepository.findById(link.getSeatId()).ifPresent(seat -> {
                seat.setStatus("BOOKED");
                seat.setHeldBy(null);
                seat.setHeldUntil(null);
                seatRepository.save(seat);
            });
        }

        List<String> ticketNumbers = new ArrayList<>();

// ── Generate tickets — one per seat ──────────────────────────────
        for (BookingSeatVO link : seatLinks) {
            seatRepository.findById(link.getSeatId()).ifPresent(seat -> {

                // ── NEW: Broadcast booking confirmation to WebSocket ──────
                seatBroadcastService.broadcastSeatUpdate(seat, "SEAT_BOOKED");

                String ticketNumber = generateTicketNumber(
                        booking.getEventId(), booking.getId(), seat.getSeatNumber()
                );


                TicketVO ticket = new TicketVO();
                ticket.setBookingId(booking.getId());
                ticket.setSeatId(seat.getId());

                ticket.setTicketNumber(
                        generateTicketNumber(
                                booking.getEventId(),
                                booking.getId(),
                                seat.getSeatNumber()
                        )
                );

                ticketRepository.save(ticket);

                ticketNumbers.add(ticketNumber);
            });
        }

        // ── NEW: Send async confirmation email ────────────────────────────
        // This call returns IMMEDIATELY — @Async on the implementation
        // hands the actual sending off to a background thread.
        // The webhook response below is NOT delayed by SMTP latency.
        Optional<UserVO> userOpt = userRepository.findById(booking.getUserId());
        Optional<EventVO> eventOpt = eventRepository.findById(booking.getEventId());

        if (userOpt.isPresent() && eventOpt.isPresent()) {
            emailService.sendBookingConfirmation(
                    userOpt.get().getEmail(),
                    booking.getBookingRef(),
                    eventOpt.get().getTitle(),
                    ticketNumbers
            );
        }

        Response response = new Response();
        response.setStatus(true);
        response.setMessage("Payment confirmed. Booking is now CONFIRMED.");

        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Ticket number format: EVT{eventId}-BKG{bookingId}-{seatNumber}
     * Example: EVT12-BKG42-A07
     */
    private String generateTicketNumber(Long eventId, Long bookingId, String seatNumber) {
        return "EVT" + eventId + "-BKG" + bookingId + "-" + seatNumber;
    }

    private PaymentInitResponseDTO buildInitResponse(
            PaymentVO payment, BigDecimal amount) {

        PaymentInitResponseDTO response = new PaymentInitResponseDTO();
        response.setRazorpayOrderId(payment.getRazorpayOrderId());
        response.setRazorpayKeyId(razorpayKeyId);
        response.setAmountInPaise(
            amount.multiply(BigDecimal.valueOf(100)).longValue()
        );
        response.setCurrency("INR");
        response.setDisplayAmount(amount);

        // Populate bookingRef from the booking
        bookingRepository.findById(payment.getBookingId())
            .ifPresent(b -> response.setBookingRef(b.getBookingRef()));

        return response;
    }
}