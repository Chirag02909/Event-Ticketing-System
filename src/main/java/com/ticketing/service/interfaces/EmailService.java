package com.ticketing.service.interfaces;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public interface EmailService {

    void sendOtpEmail(@Email @NotBlank String email, String otp, String subject);

    /**
     * Sends a booking confirmation email with ticket details.
     * Marked @Async in the implementation — caller does not wait for this
     * to complete. Safe to call from inside a webhook handler without
     * risking Razorpay's 5-second response window.
     *
     * @param toEmail        recipient
     * @param bookingRef     human-readable booking reference (e.g. TKT-20240615-00042)
     * @param eventTitle     name of the event
     * @param ticketNumbers  list of generated ticket numbers, one per seat
     */
    void sendBookingConfirmation(
            String toEmail, String bookingRef, String eventTitle, List<String> ticketNumbers
    );
}
