package com.ticketing.model;

import jakarta.persistence.*;

/**
 * Represents a physical venue where events are held.
 *
 * A venue is created once by an organiser and reused across multiple events.
 * The seat LAYOUT (rows, columns, categories) is defined at the venue level
 * but actual SeatVO rows are generated per-event — so each event gets
 * its own independent seat state (available/held/booked).
 *
 * total_capacity is a denormalised count stored here for quick capacity
 * checks without counting seat rows every time.
 */
@Entity
@Table(name = "venues")
public class VenueVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "address", nullable = false, length = 300)
    private String address;

    @Column(name = "total_capacity", nullable = false)
    private int totalCapacity;

    /**
     * The organiser who registered this venue.
     * Stored as a plain FK — we do NOT use @ManyToOne here to avoid
     * accidental lazy-load N+1 issues in list queries.
     */
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public int getTotalCapacity() { return totalCapacity; }
    public void setTotalCapacity(int totalCapacity) { this.totalCapacity = totalCapacity; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}