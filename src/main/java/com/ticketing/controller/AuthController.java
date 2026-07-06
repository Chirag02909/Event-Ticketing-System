package com.ticketing.controller;

import com.ticketing.dto.*;
import com.ticketing.dto.auth.*;
import com.ticketing.service.interfaces.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin( origins = "http://localhost:5173" , allowCredentials = "true", allowedHeaders = "*" , methods = {RequestMethod.GET,RequestMethod.POST,RequestMethod.DELETE,RequestMethod.PUT,RequestMethod.OPTIONS,RequestMethod.HEAD})
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/sendOtp")
    public ResponseEntity verifyEmail(@Valid @RequestBody SendOtpDTO sendOtpDTO) {
        return authService.verifyEmail(sendOtpDTO);
    }

    @PostMapping("/signup")
    public ResponseEntity signup(@Valid @RequestBody SignupDTO signupDTO) {
        return authService.signup(signupDTO);
    }

    @PostMapping("/login")
    public ResponseEntity login(@Valid @RequestBody LoginDTO loginDTO) {
        return authService.login(loginDTO);
    }

    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity sentResetOtp(@Valid @RequestBody SendOtpDTO sendOtpDTO) {
        return authService.sentResetOtp(sendOtpDTO);
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity resetPassword(@Valid @RequestBody ResetPasswordDTO resetPasswordDTO) {
        return authService.resetPassword(resetPasswordDTO);
    }

}
