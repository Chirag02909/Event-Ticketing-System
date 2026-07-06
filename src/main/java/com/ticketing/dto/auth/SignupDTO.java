package com.ticketing.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SignupDTO {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 16, message = "Password must be between 6 and 16 characters")
    private String password;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 6, message = "OTP must be of 6 characters")
    private String otp;

    /**
     * Only "USER" or "ORGANISER" are accepted via the public signup API.
     *
     * "ADMIN" is intentionally blocked here — admin accounts are either:
     *   1. Seeded directly in the database at deployment time
     *   2. Promoted by an existing ADMIN via POST /api/admin/users/{id}/promote
     *
     * This @Pattern is the first line of defence. The service layer
     * (AuthServiceImpl in Chunk 2) adds a second check so even if this
     * annotation is bypassed, the service rejects "ADMIN" explicitly.
     *
     * Valid values: "USER", "ORGANISER"
     */
    @NotBlank(message = "Role is required")
//    @Pattern(
//            regexp = "^(USER|ORGANISER)$",
//            message = "Role must be either USER or ORGANISER"
//    )
    private String role;

    public @NotBlank(message = "Username is required") @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters") String getUsername() {
        return username;
    }

    public void setUsername(@NotBlank(message = "Username is required") @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters") String username) {
        this.username = username;
    }

    public @NotBlank(message = "Email is required") @Email(message = "Email must be a valid email address") String getEmail() {
        return email;
    }

    public void setEmail(@NotBlank(message = "Email is required") @Email(message = "Email must be a valid email address") String email) {
        this.email = email;
    }

    public @NotBlank(message = "Password is required") @Size(min = 6, max = 16, message = "Password must be between 6 and 16 characters") String getPassword() {
        return password;
    }

    public void setPassword(@NotBlank(message = "Password is required") @Size(min = 6, max = 16, message = "Password must be between 6 and 16 characters") String password) {
        this.password = password;
    }

    public @NotBlank(message = "Password is required") @Size(min = 6, max = 6, message = "OTP must be of 6 characters") String getOtp() {
        return otp;
    }

    public void setOtp(@NotBlank(message = "Password is required") @Size(min = 6, max = 6, message = "OTP must be of 6 characters") String otp) {
        this.otp = otp;
    }

    public @NotBlank(message = "Role is required")
    String getRole() {
        return role;
    }

    public void setRole(@NotBlank(message = "Role is required") String role) {
        this.role = role;
    }
}
