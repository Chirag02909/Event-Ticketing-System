package com.ticketing.service.impl;

import com.ticketing.service.interfaces.EmailService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendOtpEmail(String email, String otp, String subject) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(buildOtpEmailBody(otp, subject));
        mailSender.send(message);
    }

    /**
     * ── @Async — THE KEY ANNOTATION FOR THIS CHUNK ──────────────────────
     *
     * When PaymentServiceImpl.confirmPayment() calls this method, Spring
     * intercepts the call and hands it to the thread pool defined in
     * AsyncConfig instead of running it on the calling thread.
     *
     * confirmPayment() does NOT wait for this method to finish — it
     * continues immediately and returns its ResponseEntity to the webhook
     * controller, which returns 200 to Razorpay within milliseconds.
     *
     * The email itself sends on a background "ticketing-async-N" thread,
     * whenever the mail server responds — even if that takes 3-4 seconds,
     * it never blocks the webhook response.
     *
     * REQUIREMENT FOR @Async TO WORK:
     * This method MUST be called from a DIFFERENT class than the one
     * it's defined in (a "self-invocation" bypasses Spring's proxy and
     * @Async is silently ignored). Since PaymentServiceImpl calls
     * EmailServiceImpl through the EmailService interface — a different
     * bean entirely — this requirement is satisfied correctly.
     */
    @Override
    @Async
    public void sendBookingConfirmation(
            String toEmail, String bookingRef, String eventTitle,
            List<String> ticketNumbers) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Booking Confirmed — " + bookingRef);
        message.setText(buildConfirmationBody(bookingRef, eventTitle, ticketNumbers));

        System.out.println("[EmailService] Sending confirmation email on thread: "
                + Thread.currentThread().getName());

        mailSender.send(message);
    }

    private String buildOtpEmailBody(String otp, String subject) {
        if (subject.toLowerCase().contains("signup") || subject.toLowerCase().contains("verify")) {
            return "Welcome to EventTicket!\n\n"
                    + "Your signup verification OTP is: " + otp + "\n\n"
                    + "This OTP is valid for 10 minutes.\n"
                    + "Do not share this with anyone.\n\n"
                    + "If you did not request this, please ignore this email.";
        }
        return "Password Reset Request\n\n"
                + "Your password reset OTP is: " + otp + "\n\n"
                + "This OTP is valid for 10 minutes.\n"
                + "Do not share this with anyone.\n\n"
                + "If you did not request this, please ignore this email.";
    }

    private String buildConfirmationBody(
            String bookingRef, String eventTitle, List<String> ticketNumbers) {

        StringBuilder body = new StringBuilder();
        body.append("Your booking is confirmed!\n\n");
        body.append("Booking Reference: ").append(bookingRef).append("\n");
        body.append("Event: ").append(eventTitle).append("\n\n");
        body.append("Your Tickets:\n");
        for (String ticketNumber : ticketNumbers) {
            body.append("  - ").append(ticketNumber).append("\n");
        }
        body.append("\nPlease present these ticket numbers at the venue entrance.\n");
        body.append("Thank you for booking with EventTicket!");

        return body.toString();
    }
}
