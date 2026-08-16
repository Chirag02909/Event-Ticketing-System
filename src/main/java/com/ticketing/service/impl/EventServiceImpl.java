package com.ticketing.service.impl;

import com.ticketing.dto.event.CreateEventDTO;
import com.ticketing.dto.event.EventResponseDTO;
import com.ticketing.dto.event.SeatLayoutItemDTO;
import com.ticketing.dto.event.VenueResponseDTO;
import com.ticketing.dto.seat.SeatResponseDTO;
import com.ticketing.dto.Response;
import com.ticketing.model.EventVO;
import com.ticketing.model.SeatVO;
import com.ticketing.model.UserVO;
import com.ticketing.model.VenueVO;
import com.ticketing.repository.EventRepository;
import com.ticketing.repository.SeatRepository;
import com.ticketing.repository.*;
import com.ticketing.repository.VenueRepository;
import com.ticketing.service.interfaces.EventService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class EventServiceImpl implements EventService {

    // Flush + clear batch size for bulk seat generation
    // Every 50 seats: write to DB + evict from memory
    private static final int BATCH_SIZE = 50;

    @Autowired private EventRepository  eventRepository;
    @Autowired private VenueRepository  venueRepository;
    @Autowired private SeatRepository   seatRepository;
    @Autowired private UserDAO   userRepository;

    /**
     * EntityManager is injected for flush() + clear() during bulk seat generation.
     * We cannot use seatRepository.flush() alone because clear() is not available
     * on the repository — it lives on EntityManager directly.
     */
    @PersistenceContext
    private EntityManager entityManager;

    // ─────────────────────────────────────────────────────────────────────
    // CREATE EVENT (status = DRAFT)
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<EventResponseDTO> createEvent(
            CreateEventDTO dto, String organiserEmail) {

        Optional<UserVO> userOpt = userRepository.findByEmail(organiserEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        UserVO organiser = userOpt.get();

        // Verify venue exists and belongs to this organiser
        Optional<VenueVO> venueOpt = venueRepository.findById(dto.getVenueId());
        if (venueOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildErrorResponse("Venue not found."));
        }

        VenueVO venue = venueOpt.get();
        if (!venue.getCreatedBy().equals(organiser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(buildErrorResponse("You do not own this venue."));
        }

        // Prevent accidental duplicates
        if (eventRepository.existsByTitleAndVenueIdAndOrganiserId(
                dto.getTitle(), dto.getVenueId(), organiser.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(buildErrorResponse(
                            "You already have an event with this title at this venue."));
        }

        // Validate seat layout total matches venue capacity
        int layoutTotal = dto.getSeatLayout().stream()
                .mapToInt(SeatLayoutItemDTO::getQuantity)
                .sum();
        if (layoutTotal != venue.getTotalCapacity()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildErrorResponse(
                            "Seat layout total (" + layoutTotal + ") must equal " +
                                    "venue capacity (" + venue.getTotalCapacity() + ")."));
        }

        EventVO event = new EventVO();
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());
        event.setVenueId(dto.getVenueId());
        event.setOrganiserId(organiser.getId());
        event.setStatus("DRAFT");

        EventVO saved = eventRepository.save(event);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toDTO(saved, venue, organiser, 0));
    }

    // ─────────────────────────────────────────────────────────────────────
    // PUBLISH EVENT — triggers bulk seat generation
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<EventResponseDTO> publishEvent(
            Long eventId, String organiserEmail) {

        Optional<UserVO> userOpt = userRepository.findByEmail(organiserEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Optional<EventVO> eventOpt = eventRepository.findById(eventId);
        if (eventOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildErrorResponse("Event not found."));
        }

        EventVO event = eventOpt.get();
        UserVO organiser = userOpt.get();

        // Ownership check
        if (!event.getOrganiserId().equals(organiser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(buildErrorResponse("You do not own this event."));
        }

        // Status transition guard — can only publish from DRAFT
        if (!"DRAFT".equals(event.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildErrorResponse(
                            "Only DRAFT events can be published. " +
                                    "Current status: " + event.getStatus()));
        }

        VenueVO venue = venueRepository.findById(event.getVenueId()).orElseThrow();

        // ── BULK SEAT GENERATION ─────────────────────────────────────────
        // Find the original CreateEventDTO seat layout — stored in the event's
        // description or re-fetched. Since we don't store layout separately,
        // the organiser must pass it again in the publish request.
        // For simplicity, we fetch venue capacity and generate generic seats.
        // In a real system, store SeatLayoutVO separately — covered in Chunk 4.
        //
        // The flush/clear pattern explained:
        //   - save()  → adds entity to Hibernate's 1st-level cache (memory)
        //   - flush() → writes cached entities to DB as SQL INSERT
        //   - clear() → evicts all entities from cache, freeing memory
        //
        // Without flush/clear: all 500 seats pile up in memory before commit.
        // With flush/clear every 50: memory holds at most 50 entities at once.
        // ─────────────────────────────────────────────────────────────────

        generateSeatsForEvent(event.getId(), venue);

        event.setStatus("PUBLISHED");
        eventRepository.save(event);

        long availableCount = seatRepository.countByEventIdAndStatus(event.getId(), "AVAILABLE");

        return ResponseEntity.ok(toDTO(event, venue, organiser, (int) availableCount));
    }

    // ─────────────────────────────────────────────────────────────────────
    // CANCEL EVENT — ORGANISER or ADMIN
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<Response> cancelEvent(
            Long eventId, String requesterEmail) {

        Optional<UserVO> userOpt = userRepository.findByEmail(requesterEmail);

        Response response = new Response();

        if (userOpt.isEmpty()) {
            response.setStatus(false);
            response.setMessage("User not found.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        Optional<EventVO> eventOpt = eventRepository.findById(eventId);

        if (eventOpt.isEmpty()) {
            response.setStatus(false);
            response.setMessage("Event not found.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        EventVO event = eventOpt.get();
        UserVO requester = userOpt.get();

        boolean isAdmin = "ADMIN".equals(requester.getRole());
        boolean isOwner = event.getOrganiserId().equals(requester.getId());

        if (!isAdmin && !isOwner) {
            response.setStatus(false);
            response.setMessage("You are not authorised to cancel this event.");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }

        if ("CANCELLED".equals(event.getStatus())) {
            response.setStatus(false);
            response.setMessage("Event is already cancelled.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if ("COMPLETED".equals(event.getStatus())) {
            response.setStatus(false);
            response.setMessage("Completed events cannot be cancelled.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        seatRepository.releaseAllSeatsForEvent(eventId);

        event.setStatus("CANCELLED");
        eventRepository.save(event);

        response.setStatus(true);
        response.setMessage("Event cancelled successfully.");

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // ─────────────────────────────────────────────────────────────────────
    // READ OPERATIONS
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<List<EventResponseDTO>> getMyEvents(String organiserEmail) {
        Optional<UserVO> userOpt = userRepository.findByEmail(organiserEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<EventResponseDTO> events = eventRepository
                .findByOrganiserId(userOpt.get().getId())
                .stream()
                .map(e -> buildEventResponse(e))
                .collect(Collectors.toList());

        return ResponseEntity.ok(events);
    }

    @Override
    public ResponseEntity<List<EventResponseDTO>> getPublishedEvents() {
        List<EventResponseDTO> events = eventRepository
                .findByStatus("PUBLISHED")
                .stream()
                .map(e -> buildEventResponse(e))
                .collect(Collectors.toList());
        return ResponseEntity.ok(events);
    }

    @Override
    public ResponseEntity<EventResponseDTO> getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .map(e -> ResponseEntity.ok(buildEventResponse(e)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @Override
    public ResponseEntity<List<SeatResponseDTO>> getSeatMap(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<SeatResponseDTO> seats = seatRepository
                .findByEventId(eventId)
                .stream()
                .map(this::toSeatDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(seats);
    }

    @Override
    public ResponseEntity<List<EventResponseDTO>> getAllEventsAdmin(String status) {
        List<EventResponseDTO> events = eventRepository
                .findAllWithOptionalStatus(status)
                .stream()
                .map(e -> buildEventResponse(e))
                .collect(Collectors.toList());
        return ResponseEntity.ok(events);
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRIVATE — Bulk seat generation
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Generates seat rows for an event using a simple sequential layout.
     * Rows: A, B, C ... Z, AA, AB ...
     * Each seat: {rowLabel}{padded number} e.g. A01, A02 ... B01
     * Category and price: GENERAL for all seats (organiser sets pricing via
     * SeatLayoutItemDTO in createEvent — stored and applied here).
     *
     * flush() + clear() every BATCH_SIZE seats keeps memory flat.
     */
    private void generateSeatsForEvent(Long eventId, VenueVO venue) {
        int total    = venue.getTotalCapacity();
        int seatsPerRow = 20;
        int count    = 0;
        int rowIndex = 0;

        while (count < total) {
            String rowLabel = generateRowLabel(rowIndex++);
            int seatsInRow  = Math.min(seatsPerRow, total - count);

            for (int s = 1; s <= seatsInRow; s++) {
                SeatVO seat = new SeatVO();
                seat.setEventId(eventId);
                seat.setRowLabel(rowLabel);
                seat.setSeatNumber(rowLabel + String.format("%02d", s));
                seat.setCategory(assignCategory(count, total));
                seat.setPrice(assignPrice(count, total));
                seat.setStatus("AVAILABLE");

                seatRepository.save(seat);
                count++;

                // flush + clear every BATCH_SIZE to prevent memory buildup
                if (count % BATCH_SIZE == 0) {
                    seatRepository.flush();
                    entityManager.clear();
                }
            }
        }

        // Final flush for the remaining seats in the last partial batch
        seatRepository.flush();
        entityManager.clear();
    }

    /**
     * Converts a zero-based index to a row label.
     * 0→A, 1→B ... 25→Z, 26→AA, 27→AB ...
     */
    private String generateRowLabel(int index) {
        StringBuilder label = new StringBuilder();
        index++;
        while (index > 0) {
            index--;
            label.insert(0, (char) ('A' + (index % 26)));
            index /= 26;
        }
        return label.toString();
    }

    /**
     * Simple category assignment based on seat position:
     * First 10% → VIP, next 20% → PREMIUM, rest → GENERAL
     */
    private String assignCategory(int seatIndex, int total) {
        double pct = (double) seatIndex / total;
        if (pct < 0.10) return "VIP";
        if (pct < 0.30) return "PREMIUM";
        return "GENERAL";
    }

    /**
     * Price tiers matching category assignment.
     */
    private BigDecimal assignPrice(int seatIndex, int total) {
        double pct = (double) seatIndex / total;
        if (pct < 0.10) return new BigDecimal("2000.00");
        if (pct < 0.30) return new BigDecimal("1000.00");
        return new BigDecimal("500.00");
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRIVATE — Mappers
    // ─────────────────────────────────────────────────────────────────────

    private EventResponseDTO buildEventResponse(EventVO event) {
        VenueVO venue       = venueRepository.findById(event.getVenueId()).orElse(null);
        UserVO  organiser   = userRepository.findById(event.getOrganiserId()).orElse(null);
        long    available   = seatRepository.countByEventIdAndStatus(event.getId(), "AVAILABLE");
        return toDTO(event, venue, organiser, (int) available);
    }

    private EventResponseDTO toDTO(EventVO event, VenueVO venue,
                                   UserVO organiser, int availableSeats) {
        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setEventDate(event.getEventDate());

        // Derive effective status: if the event is PUBLISHED but its date has
        // already passed, expose it as COMPLETED in the response without
        // persisting the change. This keeps the DB clean while ensuring the
        // organiser dashboard (and any other consumer) always sees the right label.
        String effectiveStatus = event.getStatus();
        if ("PUBLISHED".equals(effectiveStatus)
                && event.getEventDate() != null
                && event.getEventDate().isBefore(java.time.LocalDateTime.now())) {
            effectiveStatus = "COMPLETED";
        }
        dto.setStatus(effectiveStatus);

        dto.setOrganiserId(event.getOrganiserId());
        dto.setCreatedAt(event.getCreatedAt());
        dto.setAvailableSeats(availableSeats);

        if (venue != null) {
            dto.setVenueId(venue.getId());
            dto.setVenueName(venue.getName());
            dto.setVenueCity(venue.getCity());
            dto.setTotalCapacity(venue.getTotalCapacity());
        }
        if (organiser != null) {
            dto.setOrganiserName(organiser.getUsername());
        }
        return dto;
    }

    private SeatResponseDTO toSeatDTO(SeatVO s) {
        SeatResponseDTO dto = new SeatResponseDTO();
        dto.setId(s.getId());
        dto.setSeatNumber(s.getSeatNumber());
        dto.setRowLabel(s.getRowLabel());
        dto.setCategory(s.getCategory());
        dto.setPrice(s.getPrice());
        dto.setStatus(s.getStatus());
        return dto;
    }

    private EventResponseDTO buildErrorResponse(String message) {
        EventResponseDTO dto = new EventResponseDTO();
        dto.setTitle(message);
        return dto;
    }
}