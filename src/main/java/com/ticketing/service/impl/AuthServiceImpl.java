    package com.ticketing.service.impl;

    import com.ticketing.config.JwtService;
    import com.ticketing.dto.auth.*;
    import com.ticketing.repository.*;
    import com.ticketing.dto.*;
    import com.ticketing.model.*;
    import com.ticketing.service.interfaces.*;
    import jakarta.transaction.Transactional;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.security.authentication.AuthenticationManager;
    import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
    import org.springframework.security.crypto.password.PasswordEncoder;
    import org.springframework.stereotype.Service;

    import java.security.SecureRandom;
    import java.time.LocalDateTime;
    import java.util.Optional;

    @Service
    @Transactional
    public class AuthServiceImpl implements AuthService {

        @Autowired
        private EmailService emailService;

        @Autowired
        private OtpDAO otpDAO;

        @Autowired
        private UserDAO userDAO;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private AuthenticationManager authenticationManager;

        @Autowired
        private JwtService jwtService;

        private static final String ROLE_ADMIN = "ADMIN";
        private static final String PROVIDER_LOCAL= "local";

        @Override
        public ResponseEntity verifyEmail(SendOtpDTO sendOtpDTO) {

            Response response = new Response();
            try {
                if (userDAO.existsByEmail(sendOtpDTO.getEmail())) {

                    response.setStatus(false);
                    response.setMessage("Email already registered");

                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }

                String otp = GenerateOtp();
                OtpVO otpVO = new OtpVO();
                otpVO.setEmail(sendOtpDTO.getEmail());
                otpVO.setOtp(otp);
                otpVO.setOtpType("OTP_TYPE_SIGNUP");
                otpVO.setExpiresAt(LocalDateTime.now().plusMinutes(10));

                otpDAO.save(otpVO);

                emailService.sendOtpEmail(sendOtpDTO.getEmail(), otp, "Signup Verification OTP");

                response.setStatus(true);
                response.setMessage("OTP sent to " + sendOtpDTO.getEmail() + ". Please verify to complete signup.");
                return new ResponseEntity(response, HttpStatus.OK);

            } catch (Exception e)
            {
                e.printStackTrace();

                response.setStatus(false);
                response.setMessage("Error: " + e.getMessage());

                return new ResponseEntity(response,HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }


        @Override
        public ResponseEntity signup(SignupDTO signupDTO) {

            Response response = new Response();

            try {

                // 1. Check if user already exists
                if (userDAO.existsByEmail(signupDTO.getEmail())) {

                    response.setStatus(false);
                    response.setMessage("Email already registered");

                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }

                // 3. Find OTP
                OtpVO otpVO = otpDAO.findByEmail(signupDTO.getEmail())
                        .orElse(null);

                if (otpVO == null) {

                    response.setStatus(false);
                    response.setMessage("OTP not found");

                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }

                // 4. Check OTP value
                if (!otpVO.getOtp().equals(signupDTO.getOtp())) {

                    response.setStatus(false);
                    response.setMessage("Invalid OTP");

                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }

                // 5. Check OTP expiry
                if (otpVO.getExpiresAt().isBefore(LocalDateTime.now())) {

                    response.setStatus(false);
                    response.setMessage("OTP expired");

                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }

                if(ROLE_ADMIN.equalsIgnoreCase(signupDTO.getRole())){

                    response.setStatus(false);
                    response.setMessage("ADMIN accounts cannot be created through self-registration.");

                    return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
                }

                String role = signupDTO.getRole().toUpperCase();

                UserVO userVO = new UserVO();
                userVO.setUsername(signupDTO.getUsername());
                userVO.setEmail(signupDTO.getEmail());
                userVO.setPassword(passwordEncoder.encode(signupDTO.getPassword()));
                userVO.setRole(role);
                userVO.setVerified(true);
                userVO.setProvider(PROVIDER_LOCAL);
                userVO.setCreatedAt(LocalDateTime.now());

                userDAO.save(userVO);

                otpDAO.deleteByEmail(otpVO.getEmail());

                response.setStatus(true);
                response.setMessage("Signup successful");

                return new ResponseEntity<>(response, HttpStatus.CREATED);

            } catch (Exception e) {

                e.printStackTrace();

                response.setStatus(false);
                response.setMessage("Error : " + e.getMessage());

                return new ResponseEntity<>(response,
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }


        @Override
        public ResponseEntity login(LoginDTO loginDTO) {

            Response response = new Response();

            try {

                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                loginDTO.getEmail(),
                                loginDTO.getPassword()
                        )
                );

                if (!userDAO.existsByEmail(loginDTO.getEmail())) {

                    response.setStatus(false);
                    response.setMessage("Email is not registered");

                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }

                Optional<UserVO> optionalUser  = userDAO.findByEmail(loginDTO.getEmail());

                UserVO user = optionalUser.get();

                String token =
                        jwtService.generateToken(user.getEmail(), user.getRole());

                response.setToken(token);
                response.setRole(user.getRole());
                response.setMessage("Login successfully");

                return new ResponseEntity<>(
                        response,
                        HttpStatus.OK
                );

            } catch (Exception e) {

                response.setStatus(false);
                response.setMessage("Invalid email or password");

                return new ResponseEntity<>(
                        response,
                        HttpStatus.UNAUTHORIZED
                );
            }
        }

        @Override
        public ResponseEntity sentResetOtp(SendOtpDTO sendOtpDTO) {

            Response response = new Response();

            if (!userDAO.existsByEmail(sendOtpDTO.getEmail())) {

                response.setStatus(false);
                response.setMessage("Email is not registered");

                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            try {

                String otp = GenerateOtp();
                OtpVO otpVO = new OtpVO();
                otpVO.setEmail(sendOtpDTO.getEmail());
                otpVO.setOtp(otp);
                otpVO.setOtpType("Reset");
                otpVO.setExpiresAt(LocalDateTime.now().plusMinutes(10));

                otpDAO.save(otpVO);

                emailService.sendOtpEmail(sendOtpDTO.getEmail(), otp, "Reset email");

                response.setStatus(true);
                response.setMessage("OTP sent to " + sendOtpDTO.getEmail() + ". Please verify to reset the password.");
                return new ResponseEntity(response, HttpStatus.OK);

            } catch (Exception e) {

                response.setStatus(false);
                response.setMessage("Invalid email or password");

                return new ResponseEntity<>(
                        response,
                        HttpStatus.UNAUTHORIZED
                );
            }
        }


        @Override
        public ResponseEntity resetPassword(ResetPasswordDTO resetPasswordDTO) {

            Response response = new Response();

            try {

                if (!userDAO.existsByEmail(resetPasswordDTO.getEmail())) {

                    response.setStatus(false);
                    response.setMessage("user not found");

                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }

                OtpVO otpVO = otpDAO.findByEmail(resetPasswordDTO.getEmail())
                        .orElse(null);

                if (otpVO == null) {

                    response.setStatus(false);
                    response.setMessage("OTP not found");

                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }

                if (!otpVO.getOtp().equals(resetPasswordDTO.getOtp())) {

                    response.setStatus(false);
                    response.setMessage("Invalid OTP");

                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }

                if (otpVO.getExpiresAt().isBefore(LocalDateTime.now())) {

                    response.setStatus(false);
                    response.setMessage("OTP expired");

                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }

                Optional<UserVO> optionalUser =
                        userDAO.findByEmail(resetPasswordDTO.getEmail());

                UserVO userVO = optionalUser.get();

                userVO.setPassword(passwordEncoder.encode(resetPasswordDTO.getPassword()));
                userVO.setUpdatedAt(LocalDateTime.now());

                userDAO.save(userVO);

                otpDAO.deleteByEmail(otpVO.getEmail());

                response.setStatus(true);
                response.setMessage("Reset the password successfully");

                return new ResponseEntity<>(response, HttpStatus.CREATED);

            } catch (Exception e) {

                e.printStackTrace();

                response.setStatus(false);
                response.setMessage("Error : " + e.getMessage());

                return new ResponseEntity<>(response,
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        //    Private Helpers
            private String GenerateOtp() {
                SecureRandom random = new SecureRandom();
                int otp = 100000 + random.nextInt(900000); // Always 6 digits
                return String.valueOf(otp);
            }
    }
