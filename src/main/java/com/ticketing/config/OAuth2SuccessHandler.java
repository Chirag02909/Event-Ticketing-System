package com.ticketing.config;

import com.ticketing.repository.UserDAO;
import com.ticketing.model.UserVO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class OAuth2SuccessHandler
        implements AuthenticationSuccessHandler {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private  JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");

        String name = oAuth2User.getAttribute("name");

        String googleId      = oAuth2User.getAttribute("sub");

        Boolean emailVerified = oAuth2User.getAttribute("email_verified");

        UserVO user =
                userDAO.findByEmail(email)
                        .orElse(null);

        if (user == null) {

            user = new UserVO();

            user.setEmail(email);
            user.setUsername(name);

            user.setPassword(
                    UUID.randomUUID().toString());
            user.setRole("USER");


            userDAO.save(user);
        }

        String role = "USER";
        String jwt =
                jwtService.generateToken(email, role);

        response.sendRedirect(
                "http://localhost:5173/oauth-success?token="
                        + jwt);
    }
}