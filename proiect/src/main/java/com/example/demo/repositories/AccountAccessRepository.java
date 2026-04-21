package com.example.demo.repositories;

import com.example.demo.domain.AccountAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountAccessRepository extends JpaRepository<AccountAccess, Long> {

    List<AccountAccess> findByUserUserIdAndStatus(int userId, String status);
    Optional<AccountAccess> findByAccountAccountIdAndUserUserIdAndStatus(Long accountId, int userId, String status);
}
