package com.ticketing.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordDTO {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min=6 ,max=6)
    private String otp;

    @Size(min=6 , max = 16)
    private String password;

    public @Email @NotBlank String getEmail() {
        return email;
    }

    public void setEmail(@Email @NotBlank String email) {
        this.email = email;
    }

    public @NotBlank @Size(min = 6, max = 6) String getOtp() {
        return otp;
    }

    public void setOtp(@NotBlank @Size(min = 6, max = 6) String otp) {
        this.otp = otp;
    }

    public @Size(min = 6, max = 16) String getPassword() {
        return password;
    }

    public void setPassword(@Size(min = 6, max = 16) String password) {
        this.password = password;
    }
}
