package com.ticketing.repository;

import com.ticketing.model.TicketVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<TicketVO, Long> {

    List<TicketVO> findByBookingId(Long bookingId);

    Optional<TicketVO> findByTicketNumber(String ticketNumber);
}