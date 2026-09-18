package com.ticketing.dto.analytics;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class EventAnalyticsSummaryDTO {
    private Long eventId;
    private String eventTitle;
    private LocalDateTime eventDate;
    private String venueName;
    private String status;
    private int totalCapacity;
    private long availableSeats;
    private long heldSeats;
    private long bookedSeats;
    private BigDecimal revenue;
    private double occupancyPercentage;
    private List<CategoryAnalyticsDTO> categoryBreakdown;

    public EventAnalyticsSummaryDTO() {}

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    public LocalDateTime getEventDate() { return eventDate; }
    public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }

    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getTotalCapacity() { return totalCapacity; }
    public void setTotalCapacity(int totalCapacity) { this.totalCapacity = totalCapacity; }

    public long getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(long availableSeats) { this.availableSeats = availableSeats; }

    public long getHeldSeats() { return heldSeats; }
    public void setHeldSeats(long heldSeats) { this.heldSeats = heldSeats; }

    public long getBookedSeats() { return bookedSeats; }
    public void setBookedSeats(long bookedSeats) { this.bookedSeats = bookedSeats; }

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }

    public double getOccupancyPercentage() { return occupancyPercentage; }
    public void setOccupancyPercentage(double occupancyPercentage) { this.occupancyPercentage = occupancyPercentage; }

    public List<CategoryAnalyticsDTO> getCategoryBreakdown() { return categoryBreakdown; }
    public void setCategoryBreakdown(List<CategoryAnalyticsDTO> categoryBreakdown) { this.categoryBreakdown = categoryBreakdown; }
}
