package com.ticketing.repository;

import com.ticketing.model.VenueVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VenueRepository extends JpaRepository<VenueVO, Long> {

    /**
     * Organisers can only manage venues they created.
     * Used to list an organiser's venues and to validate ownership
     * before allowing event creation at a venue.
     */
    List<VenueVO> findByCreatedBy(Long createdBy);

    boolean existsByNameAndCity(String name, String city);
}