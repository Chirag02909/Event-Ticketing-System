package com.ticketing.dto.event;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Defines one pricing tier/category in a venue's seat layout.
 *
 * Example: { rowPrefix: "A", category: "GENERAL", quantity: 200, price: 500.00 }
 *
 * When the event is published, EventServiceImpl uses this list to bulk-generate
 * SeatVO rows. For the example above it generates:
 *   A01, A02, A03 ... A200  (all GENERAL, all priced at ₹500)
 */
public class SeatLayoutItemDTO {

    /**
     * Row prefix label used to name seats in this category.
     * e.g. "A" → seats A01, A02 ... A{quantity}
     *      "VIP" → seats VIP01, VIP02 ... VIP{quantity}
     */
    @NotBlank(message = "Row prefix is required")
    @Size(max = 5, message = "Row prefix must not exceed 5 characters")
    private String rowPrefix;

    /**
     * "GENERAL", "PREMIUM", or "VIP"
     */
    @NotBlank(message = "Category is required")
    private String category;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 50000, message = "Quantity per category must not exceed 50,000")
    private int quantity;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    public String getRowPrefix() { return rowPrefix; }
    public void setRowPrefix(String rowPrefix) { this.rowPrefix = rowPrefix; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}