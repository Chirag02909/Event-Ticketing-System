package com.ticketing.service.impl;

import com.ticketing.dto.analytics.AttendeeDTO;
import com.ticketing.dto.analytics.CategoryAnalyticsDTO;
import com.ticketing.dto.analytics.EventAnalyticsSummaryDTO;
import com.ticketing.dto.analytics.OrganiserAnalyticsDTO;
import com.ticketing.model.*;
import com.ticketing.repository.*;
import com.ticketing.service.interfaces.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingSeatRepository bookingSeatRepository;

    @Autowired
    private WaitlistRepository waitlistRepository;

    @Override
    public ResponseEntity<OrganiserAnalyticsDTO> getOrganiserAnalytics(String email) {
        UserVO organiser = userDAO.findByEmail(email).orElse(null);
        if (organiser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<EventVO> events = eventRepository.findByOrganiserId(organiser.getId());
        List<EventAnalyticsSummaryDTO> eventSummaries = new ArrayList<>();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        long totalTicketsSold = 0;
        double sumOccupancy = 0;
        int activeEventCountWithCapacity = 0;
        long totalWaitlist = 0;

        for (EventVO event : events) {
            EventAnalyticsSummaryDTO summary = new EventAnalyticsSummaryDTO();
            summary.setEventId(event.getId());
            summary.setEventTitle(event.getTitle());
            summary.setEventDate(event.getEventDate());
            summary.setStatus(event.getStatus());

            // Load Venue Details & Capacity
            VenueVO venue = venueRepository.findById(event.getVenueId()).orElse(null);
            int capacity = venue != null ? venue.getTotalCapacity() : 0;
            summary.setVenueName(venue != null ? venue.getName() : "Unknown Venue");

            // Seat counts & category breakdown
            List<SeatVO> seats = seatRepository.findByEventId(event.getId());
            if (seats != null && !seats.isEmpty()) {
                capacity = seats.size(); // Use actual generated seat count if available
            }
            summary.setTotalCapacity(capacity);

            long available = 0;
            long held = 0;
            long booked = 0;

            Map<String, CategoryTracker> categoryMap = new LinkedHashMap<>();

            for (SeatVO seat : seats) {
                String cat = seat.getCategory() != null ? seat.getCategory().toUpperCase() : "GENERAL";
                categoryMap.putIfAbsent(cat, new CategoryTracker(cat));
                CategoryTracker tracker = categoryMap.get(cat);
                tracker.totalSeats++;

                if ("BOOKED".equalsIgnoreCase(seat.getStatus())) {
                    booked++;
                    tracker.bookedSeats++;
                    if (seat.getPrice() != null) {
                        tracker.categoryRevenue = tracker.categoryRevenue.add(seat.getPrice());
                    }
                } else if ("HELD".equalsIgnoreCase(seat.getStatus())) {
                    held++;
                } else {
                    available++;
                }
            }

            summary.setAvailableSeats(available);
            summary.setHeldSeats(held);
            summary.setBookedSeats(booked);

            // Compute event revenue from confirmed bookings
            List<BookingVO> bookings = bookingRepository.findByEventId(event.getId());
            BigDecimal eventRevenue = BigDecimal.ZERO;
            for (BookingVO b : bookings) {
                if ("CONFIRMED".equalsIgnoreCase(b.getStatus()) && b.getTotalAmount() != null) {
                    eventRevenue = eventRevenue.add(b.getTotalAmount());
                }
            }
            summary.setRevenue(eventRevenue);

            // Occupancy percentage
            double occupancy = 0.0;
            if (capacity > 0) {
                occupancy = (booked * 100.0) / capacity;
                sumOccupancy += occupancy;
                activeEventCountWithCapacity++;
            }
            summary.setOccupancyPercentage(Math.round(occupancy * 10.0) / 10.0);

            // Category breakdown list
            List<CategoryAnalyticsDTO> catDTOList = new ArrayList<>();
            for (CategoryTracker tracker : categoryMap.values()) {
                catDTOList.add(new CategoryAnalyticsDTO(
                        tracker.category,
                        tracker.totalSeats,
                        tracker.bookedSeats,
                        tracker.categoryRevenue
                ));
            }
            summary.setCategoryBreakdown(catDTOList);

            eventSummaries.add(summary);

            totalRevenue = totalRevenue.add(eventRevenue);
            totalTicketsSold += booked;

            // Waitlist count
            List<WaitlistEntryVO> waitingList = waitlistRepository.findByEventIdAndStatus(event.getId(), "WAITING");
            totalWaitlist += (waitingList != null ? waitingList.size() : 0);
        }

        OrganiserAnalyticsDTO analytics = new OrganiserAnalyticsDTO();
        analytics.setTotalRevenue(totalRevenue);
        analytics.setTotalTicketsSold(totalTicketsSold);
        analytics.setTotalEvents(events.size());

        double avgOccupancy = activeEventCountWithCapacity > 0 ? (sumOccupancy / activeEventCountWithCapacity) : 0.0;
        analytics.setAverageOccupancyRate(Math.round(avgOccupancy * 10.0) / 10.0);
        analytics.setTotalWaitlistCount(totalWaitlist);
        analytics.setEventSummaries(eventSummaries);

        return ResponseEntity.ok(analytics);
    }

    @Override
    public ResponseEntity<List<AttendeeDTO>> getEventAttendees(Long eventId, String email) {
        UserVO organiser = userDAO.findByEmail(email).orElse(null);
        if (organiser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        EventVO event = eventRepository.findById(eventId).orElse(null);
        if (event == null) {
            return ResponseEntity.notFound().build();
        }

        // Verify organiser ownership unless user is ADMIN
        if (!event.getOrganiserId().equals(organiser.getId()) && !"ADMIN".equalsIgnoreCase(organiser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<BookingVO> bookings = bookingRepository.findByEventId(eventId);
        List<AttendeeDTO> attendees = new ArrayList<>();

        for (BookingVO b : bookings) {
            if ("CANCELLED".equalsIgnoreCase(b.getStatus())) {
                continue; // Exclude cancelled holds/bookings from roster
            }

            UserVO attendeeUser = userDAO.findById(b.getUserId()).orElse(null);
            String userName = attendeeUser != null ? attendeeUser.getUsername() : "Guest";
            String userEmail = attendeeUser != null ? attendeeUser.getEmail() : "N/A";

            // Find seat numbers for this booking
            List<BookingSeatVO> bookingSeats = bookingSeatRepository.findByBookingId(b.getId());
            List<String> seatNumbers = new ArrayList<>();
            for (BookingSeatVO bs : bookingSeats) {
                seatRepository.findById(bs.getSeatId()).ifPresent(s -> seatNumbers.add(s.getSeatNumber()));
            }

            String seatsStr = String.join(", ", seatNumbers);
            if (seatsStr.isEmpty()) {
                seatsStr = "Seats Reserved (" + b.getTotalSeats() + ")";
            }

            attendees.add(new AttendeeDTO(
                    b.getId(),
                    b.getBookingRef() != null ? b.getBookingRef() : "TKT-" + b.getId(),
                    userName,
                    userEmail,
                    seatsStr,
                    b.getTotalSeats(),
                    b.getTotalAmount(),
                    b.getStatus(),
                    b.getCreatedAt()
            ));
        }

        // Sort attendees by booking date descending
        attendees.sort((a, b) -> b.getBookingDate().compareTo(a.getBookingDate()));

        return ResponseEntity.ok(attendees);
    }

    private static class CategoryTracker {
        String category;
        long totalSeats = 0;
        long bookedSeats = 0;
        BigDecimal categoryRevenue = BigDecimal.ZERO;

        CategoryTracker(String category) {
            this.category = category;
        }
    }
}
