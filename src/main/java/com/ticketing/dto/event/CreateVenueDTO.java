package com.ticketing.dto.event;

import jakarta.validation.constraints.*;

public class CreateVenueDTO {

    @NotBlank(message = "Venue name is required")
    @Size(max = 150, message = "Venue name must not exceed 150 characters")
    private String name;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @NotBlank(message = "Address is required")
    @Size(max = 300, message = "Address must not exceed 300 characters")
    private String address;

    /**
     * Total seat capacity. The organiser defines the seat layout
     * via SeatLayoutItemDTO rows below.
     * total_capacity must equal the sum of all layout item quantities.
     */
    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 100000, message = "Capacity must not exceed 100,000")
    private int totalCapacity;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public int getTotalCapacity() { return totalCapacity; }
    public void setTotalCapacity(int totalCapacity) { this.totalCapacity = totalCapacity; }
}