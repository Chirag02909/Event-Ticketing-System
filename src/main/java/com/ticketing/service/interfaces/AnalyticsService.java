package com.ticketing.service.interfaces;

import com.ticketing.dto.analytics.AttendeeDTO;
import com.ticketing.dto.analytics.OrganiserAnalyticsDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface AnalyticsService {
    ResponseEntity<OrganiserAnalyticsDTO> getOrganiserAnalytics(String email);
    ResponseEntity<List<AttendeeDTO>> getEventAttendees(Long eventId, String email);
}
