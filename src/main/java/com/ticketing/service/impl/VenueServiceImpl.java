package com.ticketing.service.impl;

import com.ticketing.dto.event.CreateVenueDTO;
import com.ticketing.dto.event.VenueResponseDTO;
import com.ticketing.dto.Response;
import com.ticketing.model.UserVO;
import com.ticketing.model.VenueVO;
import com.ticketing.repository.UserDAO;
import com.ticketing.repository.VenueRepository;
import com.ticketing.service.interfaces.VenueService;
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
public class VenueServiceImpl implements VenueService {

    @Autowired private VenueRepository venueRepository;
    @Autowired private UserDAO userRepository;

    @Override
    public ResponseEntity<VenueResponseDTO> createVenue(
            CreateVenueDTO dto, String organiserEmail) {

        // Resolve organiser ID from email
        Optional<UserVO> userOpt = userRepository.findByEmail(organiserEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Long organiserId = userOpt.get().getId();

        // Prevent duplicate venue (same name + city)
        if (venueRepository.existsByNameAndCity(dto.getName(), dto.getCity())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(null);
        }

        VenueVO venue = new VenueVO();
        venue.setName(dto.getName());
        venue.setCity(dto.getCity());
        venue.setAddress(dto.getAddress());
        venue.setTotalCapacity(dto.getTotalCapacity());
        venue.setCreatedBy(organiserId);

        VenueVO saved = venueRepository.save(venue);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved));
    }

    @Override
    public ResponseEntity<List<VenueResponseDTO>> getMyVenues(String organiserEmail) {
        Optional<UserVO> userOpt = userRepository.findByEmail(organiserEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<VenueResponseDTO> venues = venueRepository
                .findByCreatedBy(userOpt.get().getId())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(venues);
    }

    @Override
    public ResponseEntity<VenueResponseDTO> getVenueById(Long venueId) {
        return venueRepository.findById(venueId)
                .map(v -> ResponseEntity.ok(toDTO(v)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // ── Mapper ────────────────────────────────────────────────────────────

    private VenueResponseDTO toDTO(VenueVO v) {
        VenueResponseDTO dto = new VenueResponseDTO();
        dto.setId(v.getId());
        dto.setName(v.getName());
        dto.setCity(v.getCity());
        dto.setAddress(v.getAddress());
        dto.setTotalCapacity(v.getTotalCapacity());
        dto.setCreatedBy(v.getCreatedBy());
        return dto;
    }
}