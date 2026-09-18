package com.ticketing.dto.analytics;

import java.math.BigDecimal;

public class CategoryAnalyticsDTO {
    private String category;
    private long totalSeats;
    private long bookedSeats;
    private BigDecimal revenueGenerated;

    public CategoryAnalyticsDTO() {}

    public CategoryAnalyticsDTO(String category, long totalSeats, long bookedSeats, BigDecimal revenueGenerated) {
        this.category = category;
        this.totalSeats = totalSeats;
        this.bookedSeats = bookedSeats;
        this.revenueGenerated = revenueGenerated;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public long getTotalSeats() { return totalSeats; }
    public void setTotalSeats(long totalSeats) { this.totalSeats = totalSeats; }

    public long getBookedSeats() { return bookedSeats; }
    public void setBookedSeats(long bookedSeats) { this.bookedSeats = bookedSeats; }

    public BigDecimal getRevenueGenerated() { return revenueGenerated; }
    public void setRevenueGenerated(BigDecimal revenueGenerated) { this.revenueGenerated = revenueGenerated; }
}
