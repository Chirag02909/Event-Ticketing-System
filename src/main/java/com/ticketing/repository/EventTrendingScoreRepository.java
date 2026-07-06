package com.ticketing.repository;

import com.ticketing.model.EventTrendingScoreVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventTrendingScoreRepository extends JpaRepository<EventTrendingScoreVO, Long> {

    /**
     * The entire read path for the trending endpoint: one indexed
     * ORDER BY. No aggregation happens here — that already happened
     * when the scheduler last ran. Pagination/limit is applied at the
     * service layer via Pageable so the limit is configurable.
     */
    @Query("SELECT t FROM EventTrendingScoreVO t ORDER BY t.velocityScore DESC")
    List<EventTrendingScoreVO> findTopTrending(org.springframework.data.domain.Pageable pageable);
}