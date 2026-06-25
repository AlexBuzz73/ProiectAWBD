package com.example.demo.services;

import com.example.demo.domain.ScheduledPayment;
import com.example.demo.repositories.ScheduledPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledPaymentJob {

    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final TransactionService transactionService;

    @Scheduled(cron = "0 0 1 * * *")
    public void processScheduledPayments() {
        List<ScheduledPayment> duePayments = scheduledPaymentRepository
                .findByStatusAndScheduledDateLessThanEqual("ACTIVE", new Date());

        log.debug("ScheduledPaymentJob: {} plati programate ajunse la scadenta.", duePayments.size());

        for (ScheduledPayment scheduledPayment : duePayments) {
            try {
                transactionService.executeTransaction(scheduledPayment.getTransaction().getTransactionId());
                scheduledPayment.setStatus("EXECUTED");
                scheduledPayment.setUpdatedAt(new Date());
                scheduledPaymentRepository.save(scheduledPayment);
                log.info("ScheduledPaymentJob: plata programata {} a fost executata.", scheduledPayment.getScheduledPaymentId());
            } catch (Exception e) {
                scheduledPayment.setStatus("FAILED");
                scheduledPayment.setUpdatedAt(new Date());
                scheduledPaymentRepository.save(scheduledPayment);
                log.error("ScheduledPaymentJob: eroare la executia platii programate {}: {}",
                        scheduledPayment.getScheduledPaymentId(), e.getMessage(), e);
            }
        }
    }
}
