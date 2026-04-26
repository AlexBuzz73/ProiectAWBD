package com.example.demo.repositories;


import com.example.demo.domain.UserLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserLimitRepository extends JpaRepository<UserLimit, Long> {

    Optional<UserLimit> findByUserUserIdAndStatus(Long UserId, String status);
}
