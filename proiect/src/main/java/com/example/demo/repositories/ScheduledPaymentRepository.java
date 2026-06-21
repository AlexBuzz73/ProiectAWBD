package com.example.demo.repositories;

import com.example.demo.domain.ScheduledPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ScheduledPaymentRepository extends JpaRepository<ScheduledPayment, Integer> {
    List<ScheduledPayment> findByStatusAndScheduledDateLessThanEqual(String status, Date date);
}