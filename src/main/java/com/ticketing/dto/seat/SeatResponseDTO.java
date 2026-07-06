package com.ticketing.dto.seat;

import java.math.BigDecimal;

/**
 * Sent to the frontend for rendering each seat on the interactive seat map.
 * Also used as the WebSocket broadcast payload when a seat status changes.
 *
 * The frontend uses:
 *   - status    → colour (green=AVAILABLE, yellow=HELD, red=BOOKED)
 *   - category  → zone colour / pricing badge
 *   - rowLabel  → which row to render this seat in
 *   - seatNumber→ label printed inside the seat circle on the map
 */
public class SeatResponseDTO {

    private Long id;
    private String seatNumber;
    private String rowLabel;
    private String category;
    private BigDecimal price;
    private String status;   // AVAILABLE / HELD / BOOKED

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

    public String getRowLabel() { return rowLabel; }
    public void setRowLabel(String rowLabel) { this.rowLabel = rowLabel; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}