package com.ticketing.repository;

import com.ticketing.model.UserVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDAO extends JpaRepository<UserVO, Long> {

    Optional<UserVO> findByEmail(String email);

    boolean existsByEmail(String email);

    void deleteByEmail(String email);

    List<UserVO> findByRole(String upperCase);
}