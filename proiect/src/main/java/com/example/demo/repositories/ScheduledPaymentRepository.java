package com.example.demo.repositories;

import com.example.demo.domain.ScheduledPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduledPaymentRepository extends JpaRepository<ScheduledPayment, Integer> {
}