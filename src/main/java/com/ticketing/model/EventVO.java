package com.ticketing.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a ticketed event at a venue.
 *
 * Lifecycle: DRAFT → PUBLISHED → CANCELLED / COMPLETED
 *
 * An event is created by an organiser. When the organiser publishes it,
 * the service layer bulk-generates SeatVO rows for this event (one per seat
 * in the venue layout). From that point, users can browse and hold seats.
 *
 * status is stored as a VARCHAR to avoid ENUM migration pain in MySQL.
 * Valid values enforced at the service layer.
 */
@Entity
@Table(name = "events")
public class EventVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    /**
     * DRAFT       - created but not yet visible to users
     * PUBLISHED   - live, seats available for booking
     * CANCELLED   - all held/booked seats will be released
     * COMPLETED   - event has passed
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "venue_id", nullable = false)
    private Long venueId;

    @Column(name = "organiser_id", nullable = false)
    private Long organiserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getEventDate() { return eventDate; }
    public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getVenueId() { return venueId; }
    public void setVenueId(Long venueId) { this.venueId = venueId; }

    public Long getOrganiserId() { return organiserId; }
    public void setOrganiserId(Long organiserId) { this.organiserId = organiserId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}