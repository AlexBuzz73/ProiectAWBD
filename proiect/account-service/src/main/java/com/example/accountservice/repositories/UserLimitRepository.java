package com.example.accountservice.repositories;

import com.example.accountservice.domain.UserLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserLimitRepository extends JpaRepository<UserLimit, Integer> {
    Optional<UserLimit> findByUserIdAndStatus(Integer userId, String status);
    Optional<UserLimit> findByUserId(Integer userId);
    void deleteByUserId(Integer userId);
}
