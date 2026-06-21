package com.example.demo.services;

import com.example.demo.domain.Transaction;
import com.example.demo.repositories.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentJob {

    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    @Scheduled(fixedDelay = 60000)
    public void processStandardPayments() {

        List<Transaction> pendingTransactions = transactionRepository
                .findByStatusAndIsUrgentAndIsScheduled("PENDING_EXECUTION", "NO", "NO");

        log.debug("PaymentJob: {} tranzactii standard de procesat.", pendingTransactions.size());

        for (Transaction t : pendingTransactions) {
            try {
                transactionService.executeTransaction(t.getTransactionId());
                log.info("PaymentJob: tranzactia {} a fost executata automat.", t.getTransactionId());
            } catch (Exception e) {
                log.error("PaymentJob: eroare la executia automata a tranzactiei {}: {}", t.getTransactionId(), e.getMessage(), e);
            }
        }
    }
}
