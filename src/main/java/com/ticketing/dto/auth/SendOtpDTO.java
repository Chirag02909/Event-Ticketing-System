package com.ticketing.dto.auth;

import jakarta.validation.constraints.Email;

public class SendOtpDTO {

    @Email
    private String email;

    public @Email String getEmail() {
        return email;
    }

    public void setEmail(@Email String email) {
        this.email = email;
    }
}
