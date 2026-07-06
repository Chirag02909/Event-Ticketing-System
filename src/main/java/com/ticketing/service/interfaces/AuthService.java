package com.ticketing.service.interfaces;

import com.ticketing.dto.*;
import com.ticketing.dto.auth.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

public interface AuthService {


    ResponseEntity verifyEmail(@Valid SendOtpDTO sendOtpDTO);

    ResponseEntity signup(@Valid SignupDTO signupDTO);

    ResponseEntity login(@Valid LoginDTO loginDTO);

    ResponseEntity sentResetOtp(@Valid SendOtpDTO sendOtpDTO);

    ResponseEntity resetPassword(@Valid ResetPasswordDTO resetPasswordDTO);
}
