package com.ticketing.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginDTO {

    @Email
    @NotBlank(message = "Email must nto be empty")
    private String email;

    @Size( min = 6, max =  16 , message = "password has atLeast 6 characters and atMax 16 characters")
    private  String password;

    public @Email @NotBlank(message = "Email must nto be empty") String getEmail() {
        return email;
    }

    public void setEmail(@Email @NotBlank(message = "Email must nto be empty") String email) {
        this.email = email;
    }

    public @Size(min = 6, max = 16, message = "password has atLeast 6 characters and atMax 16 characters") String getPassword() {
        return password;
    }

    public void setPassword(@Size(min = 6, max = 16, message = "password has atLeast 6 characters and atMax 16 characters") String password) {
        this.password = password;
    }
}
