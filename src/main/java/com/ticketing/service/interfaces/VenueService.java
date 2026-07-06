package com.ticketing.service.interfaces;

import com.ticketing.dto.event.CreateVenueDTO;
import com.ticketing.dto.event.VenueResponseDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface VenueService {

    ResponseEntity<VenueResponseDTO> createVenue(CreateVenueDTO dto, String organiserEmail);

    ResponseEntity<List<VenueResponseDTO>> getMyVenues(String organiserEmail);

    ResponseEntity<VenueResponseDTO> getVenueById(Long venueId);
}