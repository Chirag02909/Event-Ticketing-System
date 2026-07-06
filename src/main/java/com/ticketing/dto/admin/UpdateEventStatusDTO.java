package com.ticketing.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdateEventStatusDTO {

    @NotBlank(message = "Status is required")
    @Pattern(
        regexp = "^(PUBLISHED|CANCELLED|COMPLETED)$",
        message = "Status must be PUBLISHED, CANCELLED, or COMPLETED"
    )
    private String status;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}