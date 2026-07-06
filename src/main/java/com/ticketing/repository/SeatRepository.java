package com.ticketing.repository;

import com.ticketing.model.SeatVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<SeatVO, Long> {

    /**
     * Fetches all seats for an event — used to render the seat map.
     * The idx_seats_event_id index (defined on SeatVO) makes this fast
     * even for venues with 500+ seats.
     */
    List<SeatVO> findByEventId(Long eventId);

    /**
     * Counts available seats — shown on event listing cards.
     */
    long countByEventIdAndStatus(Long eventId, String status);

    /**
     * Used by the hold expiry scheduler (Chunk 4).
     * Finds all seats that are HELD but whose hold has expired.
     */
    @Query("SELECT s FROM SeatVO s " +
            "WHERE s.status = 'HELD' " +
            "AND s.heldUntil < :now")
    List<SeatVO> findExpiredHeldSeats(@Param("now") LocalDateTime now);

    /**
     * Bulk-releases all seats for a cancelled event.
     * One UPDATE statement instead of loading and saving each seat individually.
     */
    @Modifying
    @Transactional
    @Query("UPDATE SeatVO s SET s.status = 'AVAILABLE', " +
            "s.heldBy = NULL, s.heldUntil = NULL " +
            "WHERE s.eventId = :eventId " +
            "AND s.status IN ('HELD', 'AVAILABLE')")
    void releaseAllSeatsForEvent(@Param("eventId") Long eventId);
}