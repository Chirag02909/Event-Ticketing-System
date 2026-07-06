package com.ticketing.service.interfaces;

import com.ticketing.dto.Response;
import com.ticketing.dto.event.CreateEventDTO;
import com.ticketing.dto.event.EventResponseDTO;
import com.ticketing.dto.seat.SeatResponseDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface EventService {

    // ── Organiser operations ─────────────────────────────────────────────

    ResponseEntity<EventResponseDTO> createEvent(CreateEventDTO dto, String organiserEmail);

    ResponseEntity<EventResponseDTO> publishEvent(Long eventId, String organiserEmail);

    ResponseEntity<Response> cancelEvent(Long eventId, String requesterEmail);

    ResponseEntity<List<EventResponseDTO>> getMyEvents(String organiserEmail);

    // ── Public operations ────────────────────────────────────────────────

    ResponseEntity<List<EventResponseDTO>> getPublishedEvents();

    ResponseEntity<EventResponseDTO> getEventById(Long eventId);

    ResponseEntity<List<SeatResponseDTO>> getSeatMap(Long eventId);

    // ── Admin operations ─────────────────────────────────────────────────

    ResponseEntity<List<EventResponseDTO>> getAllEventsAdmin(String status);
}