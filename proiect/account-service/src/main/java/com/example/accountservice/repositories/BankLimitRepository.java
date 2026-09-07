package com.example.accountservice.repositories;

import com.example.accountservice.domain.BankLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankLimitRepository extends JpaRepository<BankLimit, Integer> {
    Optional<BankLimit> findByStatus(String status);
}
