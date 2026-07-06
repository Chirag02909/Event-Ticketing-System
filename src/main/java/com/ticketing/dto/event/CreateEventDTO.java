package com.ticketing.dto.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public class CreateEventDTO {

    @NotBlank(message = "Event title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotNull(message = "Event date is required")
    @Future(message = "Event date must be in the future")
    private LocalDateTime eventDate;

    @NotNull(message = "Venue ID is required")
    private Long venueId;

    /**
     * One item per pricing category.
     * Validated with @Valid so each SeatLayoutItemDTO's constraints are checked.
     * At least one category must be defined.
     */
    @NotNull(message = "Seat layout is required")
    @Size(min = 1, message = "At least one seat category must be defined")
    @Valid
    private List<SeatLayoutItemDTO> seatLayout;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getEventDate() { return eventDate; }
    public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }

    public Long getVenueId() { return venueId; }
    public void setVenueId(Long venueId) { this.venueId = venueId; }

    public List<SeatLayoutItemDTO> getSeatLayout() { return seatLayout; }
    public void setSeatLayout(List<SeatLayoutItemDTO> seatLayout) { this.seatLayout = seatLayout; }
}