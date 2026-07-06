package com.ticketing.repository;

import com.ticketing.model.BookingVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<BookingVO, Long> {

    Optional<BookingVO> findByBookingRef(String bookingRef);

    List<BookingVO> findByUserId(Long userId);

    List<BookingVO> findByEventId(Long eventId);

    /**
     * Used by payment webhook to find the pending booking
     * that needs to be confirmed after Razorpay fires.
     */
    Optional<BookingVO> findByIdAndStatus(Long id, String status);

    /**
     * ADDED FOR CHUNK 10 — the core aggregation query behind trending
     * velocity. Returns one row per event: [eventId, count] for every
     * event that had at least one CONFIRMED booking inside the window.
     *
     * Only CONFIRMED bookings count — see EventTrendingScoreVO's class
     * Javadoc for why PENDING_PAYMENT and CANCELLED/REFUNDED are
     * deliberately excluded.
     *
     * Returns Object[] rather than a DTO because JPQL constructor
     * expressions for grouped aggregates need either a dedicated
     * projection interface or this simpler array form — for a single
     * internal scheduler query, the array form is the pragmatic choice.
     * Object[0] = eventId (Long), Object[1] = count (Long).
     */
    @Query("SELECT b.eventId, COUNT(b) FROM BookingVO b " +
            "WHERE b.status = 'CONFIRMED' " +
            "AND b.createdAt > :since " +
            "GROUP BY b.eventId")
    List<Object[]> countConfirmedBookingsPerEventSince(@Param("since") LocalDateTime since);
}