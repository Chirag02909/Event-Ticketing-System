package com.ticketing.repository;

import com.ticketing.model.EventVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<EventVO, Long> {

    /**
     * Public event browsing — only PUBLISHED events are visible to users.
     */
    List<EventVO> findByStatus(String status);

    /**
     * Organiser's own event list — all statuses including DRAFT.
     */
    List<EventVO> findByOrganiserId(Long organiserId);

    /**
     * Admin view — all events across all organisers.
     * Optional status filter: pass null to get all.
     */
    @Query("SELECT e FROM EventVO e WHERE (:status IS NULL OR e.status = :status) " +
            "ORDER BY e.createdAt DESC")
    List<EventVO> findAllWithOptionalStatus(@Param("status") String status);

    /**
     * Check if an organiser already has an event with the same title
     * at the same venue on the same date — prevents accidental duplicates.
     */
    boolean existsByTitleAndVenueIdAndOrganiserId(
            String title, Long venueId, Long organiserId
    );
}