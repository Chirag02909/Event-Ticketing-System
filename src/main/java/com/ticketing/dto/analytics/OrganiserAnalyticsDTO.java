package com.ticketing.dto.analytics;

import java.math.BigDecimal;
import java.util.List;

public class OrganiserAnalyticsDTO {
    private BigDecimal totalRevenue;
    private long totalTicketsSold;
    private int totalEvents;
    private double averageOccupancyRate;
    private long totalWaitlistCount;
    private List<EventAnalyticsSummaryDTO> eventSummaries;

    public OrganiserAnalyticsDTO() {}

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public long getTotalTicketsSold() { return totalTicketsSold; }
    public void setTotalTicketsSold(long totalTicketsSold) { this.totalTicketsSold = totalTicketsSold; }

    public int getTotalEvents() { return totalEvents; }
    public void setTotalEvents(int totalEvents) { this.totalEvents = totalEvents; }

    public double getAverageOccupancyRate() { return averageOccupancyRate; }
    public void setAverageOccupancyRate(double averageOccupancyRate) { this.averageOccupancyRate = averageOccupancyRate; }

    public long getTotalWaitlistCount() { return totalWaitlistCount; }
    public void setTotalWaitlistCount(long totalWaitlistCount) { this.totalWaitlistCount = totalWaitlistCount; }

    public List<EventAnalyticsSummaryDTO> getEventSummaries() { return eventSummaries; }
    public void setEventSummaries(List<EventAnalyticsSummaryDTO> eventSummaries) { this.eventSummaries = eventSummaries; }
}
