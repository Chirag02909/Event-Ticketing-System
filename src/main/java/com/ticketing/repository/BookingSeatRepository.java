package com.ticketing.repository;

import com.ticketing.model.BookingSeatVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeatVO, Long> {

    List<BookingSeatVO> findByBookingId(Long bookingId);

    List<BookingSeatVO> findBySeatId(Long seatId);
}