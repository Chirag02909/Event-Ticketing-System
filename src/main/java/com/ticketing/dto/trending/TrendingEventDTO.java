package com.ticketing.dto.trending;

import java.time.LocalDateTime;

/**
 * One entry in the trending events list. Combines the pre-computed
 * score with enough event detail to render a card on the frontend
 * without a second round-trip per event.
 */
public class TrendingEventDTO {

    private Long eventId;
    private String title;
    private String venueName;
    private String venueCity;
    private LocalDateTime eventDate;
    private int availableSeats;

    private double velocityScore;
    private int bookingCountInWindow;
    private int windowHours;

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }

    public String getVenueCity() { return venueCity; }
    public void setVenueCity(String venueCity) { this.venueCity = venueCity; }

    public LocalDateTime getEventDate() { return eventDate; }
    public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }

    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    public double getVelocityScore() { return velocityScore; }
    public void setVelocityScore(double velocityScore) { this.velocityScore = velocityScore; }

    public int getBookingCountInWindow() { return bookingCountInWindow; }
    public void setBookingCountInWindow(int bookingCountInWindow) { this.bookingCountInWindow = bookingCountInWindow; }

    public int getWindowHours() { return windowHours; }
    public void setWindowHours(int windowHours) { this.windowHours = windowHours; }
}