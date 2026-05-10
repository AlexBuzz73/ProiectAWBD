package com.example.demo.repositories;

import com.example.demo.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    List<Transaction> findByStatusAndIsUrgentAndIsScheduled(String status, String isUrgent, String isScheduled);
}
