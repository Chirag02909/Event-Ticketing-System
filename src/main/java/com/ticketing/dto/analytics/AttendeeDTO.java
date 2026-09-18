package com.ticketing.dto.analytics;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AttendeeDTO {
    private Long bookingId;
    private String bookingRef;
    private String userName;
    private String userEmail;
    private String seatNumbers;
    private int seatCount;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime bookingDate;

    public AttendeeDTO() {}

    public AttendeeDTO(Long bookingId, String bookingRef, String userName, String userEmail,
                       String seatNumbers, int seatCount, BigDecimal totalAmount,
                       String status, LocalDateTime bookingDate) {
        this.bookingId = bookingId;
        this.bookingRef = bookingRef;
        this.userName = userName;
        this.userEmail = userEmail;
        this.seatNumbers = seatNumbers;
        this.seatCount = seatCount;
        this.totalAmount = totalAmount;
        this.status = status;
        this.bookingDate = bookingDate;
    }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getBookingRef() { return bookingRef; }
    public void setBookingRef(String bookingRef) { this.bookingRef = bookingRef; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getSeatNumbers() { return seatNumbers; }
    public void setSeatNumbers(String seatNumbers) { this.seatNumbers = seatNumbers; }

    public int getSeatCount() { return seatCount; }
    public void setSeatCount(int seatCount) { this.seatCount = seatCount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDateTime bookingDate) { this.bookingDate = bookingDate; }
}
