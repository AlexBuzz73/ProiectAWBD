package com.example.demo.services;

import com.example.demo.domain.Transaction;
import com.example.demo.repositories.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentJob {

    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    @Scheduled(fixedDelay = 60000)
    public void processStandardPayments() {

        List<Transaction> pendingTransactions = transactionRepository
                .findByStatusAndIsUrgentAndIsScheduled("AUTHORIZED", "NO", "NO");

        for (Transaction t : pendingTransactions) {
            try {

                transactionService.executeTransaction(t.getTransactionId());
                System.out.println("Job: Tranzactia " + t.getTransactionId() + " a fost executata automat.");
            } catch (Exception e) {
                System.err.println("Eroare la executia automata: " + e.getMessage());
            }
        }
    }
}