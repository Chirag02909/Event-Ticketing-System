package com.ticketing.dto.booking;

import com.ticketing.dto.seat.SeatResponseDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class BookingResponseDTO {

    private Long id;
    private String bookingRef;
    private String status;
    private int totalSeats;
    private BigDecimal totalAmount;
    private Long eventId;
    private String eventTitle;
    private LocalDateTime eventDate;
    private String venueName;
    private List<SeatResponseDTO> seats;

    // Payment info — populated after payment initiation
    private String razorpayOrderId;

    // Ticket numbers — populated after payment confirmation
    private List<String> ticketNumbers;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBookingRef() { return bookingRef; }
    public void setBookingRef(String bookingRef) { this.bookingRef = bookingRef; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    public LocalDateTime getEventDate() { return eventDate; }
    public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }

    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }

    public List<SeatResponseDTO> getSeats() { return seats; }
    public void setSeats(List<SeatResponseDTO> seats) { this.seats = seats; }

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }

    public List<String> getTicketNumbers() { return ticketNumbers; }
    public void setTicketNumbers(List<String> ticketNumbers) { this.ticketNumbers = ticketNumbers; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}