package com.ticketing.service.impl;

import com.ticketing.dto.Response;
import com.ticketing.dto.admin.*;
import com.ticketing.model.UserVO;
import com.ticketing.repository.UserDAO;
import com.ticketing.service.interfaces.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserDAO userRepository;

    @Override
    public ResponseEntity<List<UserSummaryDTO>> getAllUsers(String role) {
        List<UserVO> users = (role != null && !role.isBlank())
                ? userRepository.findByRole(role.toUpperCase())
                : userRepository.findAll();

        return ResponseEntity.ok(
                users.stream().map(this::toSummary).collect(Collectors.toList())
        );
    }

    @Override
    public ResponseEntity<UserSummaryDTO> getUserById(Long userId) {
        return userRepository.findById(userId)
                .map(user -> ResponseEntity.ok(toSummary(user)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @Override
    public ResponseEntity<Response> updateUserRole(
            Long userId, PromoteUserDTO promoteUserDTO) {

        Optional<UserVO> userOpt = userRepository.findById(userId);

        Response response = new Response();

        if (userOpt.isEmpty()) {
            response.setStatus(false);
            response.setMessage("User not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        UserVO user = userOpt.get();
        String newRole = promoteUserDTO.getRole().toUpperCase();

        if ("ADMIN".equals(user.getRole())) {
            response.setStatus(false);
            response.setMessage("Admin role cannot be modified");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }

        if ("ADMIN".equals(newRole)) {
            response.setStatus(false);
            response.setMessage("Cannot promote user to ADMIN");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }

        if (user.getRole().equals(newRole)) {
            response.setStatus(false);
            response.setMessage("User already has role : " + newRole);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        user.setRole(newRole);
        userRepository.save(user);

        response.setStatus(true);
        response.setMessage("User role updated successfully");
        response.setRole(newRole);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Response> deleteUser(Long userId) {
        Optional<UserVO> userOpt = userRepository.findById(userId);

        Response response = new Response();

        if (userOpt.isEmpty()) {
            response.setStatus(false);
            response.setMessage("User not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        UserVO user = userOpt.get();

        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            response.setStatus(false);
            response.setMessage("Admin accounts cannot be deleted via this endpoint");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }

        userRepository.delete(user);

        response.setStatus(true);
        response.setMessage("User " + user.getEmail() + " deleted successfully");

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private UserSummaryDTO toSummary(UserVO user) {
        UserSummaryDTO dto = new UserSummaryDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setVerified(user.isVerified());
        dto.setProvider(user.getProvider());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}