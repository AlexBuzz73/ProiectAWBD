package com.example.demo.repositories;

import com.example.demo.domain.AccountAccess;
import com.example.demo.domain.BankLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankLimitRepository extends JpaRepository<BankLimit, Long> {

    Optional<BankLimit> findByStatus(String status);
}
