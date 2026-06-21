package com.example.demo.services;

import com.example.demo.domain.ScheduledPayment;
import com.example.demo.repositories.ScheduledPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Job zilnic care executa platile programate (SCHEDULED_PAYMENTS) ajunse la scadenta.
 * Conform doc/flows/7-transactions.md (sectiunea 8, Caz 3) si doc/flows/9-exchanges.md (sectiunea 13):
 * executia se face la data specificata, printr-un job ce ruleaza zilnic.
 *
 * Ruleaza in fiecare zi la 01:00. Pentru a testa rapid in dev, se poate inlocui temporar
 * cron-ul cu un fixedDelay mai mic (ex: fixedDelay = 60000).
 */
@Service
@RequiredArgsConstructor
public class ScheduledPaymentJob {

    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final TransactionService transactionService;

    @Scheduled(cron = "0 0 1 * * *")
    public void processScheduledPayments() {
        List<ScheduledPayment> duePayments = scheduledPaymentRepository
                .findByStatusAndScheduledDateLessThanEqual("ACTIVE", new Date());

        for (ScheduledPayment scheduledPayment : duePayments) {
            try {
                transactionService.executeTransaction(scheduledPayment.getTransaction().getTransactionId());
                scheduledPayment.setStatus("EXECUTED");
                scheduledPayment.setUpdatedAt(new Date());
                scheduledPaymentRepository.save(scheduledPayment);
                System.out.println("Job: Plata programata " + scheduledPayment.getScheduledPaymentId() + " a fost executata.");
            } catch (Exception e) {
                scheduledPayment.setStatus("FAILED");
                scheduledPayment.setUpdatedAt(new Date());
                scheduledPaymentRepository.save(scheduledPayment);
                System.err.println("Job: Eroare la executia platii programate " + scheduledPayment.getScheduledPaymentId() + ": " + e.getMessage());
            }
        }
    }
}
