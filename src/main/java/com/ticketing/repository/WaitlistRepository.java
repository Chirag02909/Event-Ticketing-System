package com.ticketing.repository;

import com.ticketing.model.WaitlistEntryVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WaitlistRepository extends JpaRepository<WaitlistEntryVO, Long> {

    List<WaitlistEntryVO> findByEventIdAndStatus(Long eventId, String status);

    List<WaitlistEntryVO> findByUserIdAndEventId(Long userId, Long eventId);

    /**
     * Prevents a user from joining the same event's waitlist twice while
     * they already have an active (WAITING or OFFERED) entry.
     */
    Optional<WaitlistEntryVO> findByUserIdAndEventIdAndStatusIn(
        Long userId, Long eventId, List<String> statuses
    );

    /**
     * Returns this user's position in line (1-indexed) among all WAITING
     * entries for the event, ordered by joinedAt. Used to show "You are
     * #4 in line" on the frontend.
     */
    @Query("SELECT COUNT(w) FROM WaitlistEntryVO w " +
           "WHERE w.eventId = :eventId AND w.status = 'WAITING' " +
           "AND w.joinedAt < (SELECT w2.joinedAt FROM WaitlistEntryVO w2 WHERE w2.id = :entryId)")
    long countAheadInLine(@Param("eventId") Long eventId, @Param("entryId") Long entryId);

    /**
     * Finds offers whose response window has passed — same pattern as
     * SeatRepository.findExpiredHeldSeats from Chunk 4.
     */
    @Query("SELECT w FROM WaitlistEntryVO w " +
           "WHERE w.status = 'OFFERED' " +
           "AND w.offerExpiresAt < :now")
    List<WaitlistEntryVO> findExpiredOffers(@Param("now") LocalDateTime now);

    // ─────────────────────────────────────────────────────────────────────
    // THE ATOMIC CLAIM — the centerpiece of this chunk
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Atomically claims the single oldest WAITING entry for an event and
     * flips it to OFFERED, in ONE indivisible SQL statement.
     *
     * WHY THIS IS SAFE UNDER CONCURRENCY (and @Version is NOT used here):
     * The inner SELECT picks the oldest WAITING row's id. The outer
     * UPDATE's "AND status = 'WAITING'" guard means that if two threads
     * call this at the exact same moment, the database's row-level
     * locking during the UPDATE ensures only ONE of them actually
     * transitions a row from WAITING to OFFERED. The second thread's
     * UPDATE either targets a row that's already OFFERED (0 rows
     * affected) or, if MySQL picked a different "oldest" row mid-race,
     * still only flips exactly one row — never the same row twice.
     *
     * This is a native query (not JPQL) because JPQL does not support
     * subqueries inside UPDATE statements the way raw SQL does — this
     * is one of the few places in the project where native SQL is the
     * correct tool over JPQL.
     *
     * @return number of rows updated — 1 if a claim succeeded, 0 if the
     *         waitlist for this event was empty (no WAITING entries).
     */
    @Modifying
    @Transactional
    @Query(value =
        "UPDATE waitlist_entries " +
        "SET status = 'OFFERED', offered_at = :now, offer_expires_at = :expiresAt " +
        "WHERE id = ( " +
        "    SELECT id FROM ( " +
        "        SELECT id FROM waitlist_entries " +
        "        WHERE event_id = :eventId AND status = 'WAITING' " +
        "        ORDER BY joined_at ASC " +
        "        LIMIT 1 " +
        "    ) AS oldest_entry " +
        ") " +
        "AND status = 'WAITING'",
        nativeQuery = true)
    int claimNextInLine(
        @Param("eventId") Long eventId,
        @Param("now") LocalDateTime now,
        @Param("expiresAt") LocalDateTime expiresAt
    );

    /**
     * Used immediately after claimNextInLine() to find out WHICH entry
     * was just claimed, so the service layer can assign a seat to it.
     * Looks for the single most-recently-offered entry for this event
     * that doesn't have a seat assigned yet.
     */
    @Query("SELECT w FROM WaitlistEntryVO w " +
           "WHERE w.eventId = :eventId AND w.status = 'OFFERED' " +
           "AND w.offeredSeatId IS NULL " +
           "ORDER BY w.offeredAt DESC")
    List<WaitlistEntryVO> findRecentlyClaimedWithoutSeat(@Param("eventId") Long eventId);
}