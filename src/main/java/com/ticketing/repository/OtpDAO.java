package com.ticketing.repository;

import com.ticketing.model.OtpVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpDAO extends JpaRepository<OtpVO, String> {

    Optional<OtpVO> findByEmail(String email);

    boolean existsByEmail(String email);

    void deleteByEmail(String email);
}