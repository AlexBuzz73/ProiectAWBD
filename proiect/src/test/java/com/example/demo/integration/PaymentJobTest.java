package com.example.demo.integration;

import com.example.demo.domain.Transaction;
import com.example.demo.repositories.TransactionRepository;
import com.example.demo.services.PaymentJob;
import com.example.demo.services.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentJobTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private PaymentJob paymentJob;


    @Test
    void processStandardPayments_noPendingTransactions_doesNothing() {

        when(transactionRepository
                .findByStatusAndIsUrgentAndIsScheduled(
                        "PENDING_EXECUTION",
                        "NO",
                        "NO"))
                .thenReturn(Collections.emptyList());

        paymentJob.processStandardPayments();

        verify(transactionRepository)
                .findByStatusAndIsUrgentAndIsScheduled(
                        "PENDING_EXECUTION",
                        "NO",
                        "NO");

        verifyNoInteractions(transactionService);
    }


    @Test
    void processStandardPayments_pendingTransactions_executesAll() {

        Transaction transaction1 = mock(Transaction.class);
        Transaction transaction2 = mock(Transaction.class);

        when(transaction1.getTransactionId()).thenReturn(1);
        when(transaction2.getTransactionId()).thenReturn(2);

        when(transactionRepository
                .findByStatusAndIsUrgentAndIsScheduled(
                        "PENDING_EXECUTION",
                        "NO",
                        "NO"))
                .thenReturn(List.of(transaction1, transaction2));

        paymentJob.processStandardPayments();

        verify(transactionService).executeTransaction(1);
        verify(transactionService).executeTransaction(2);
    }


    @Test
    void processStandardPayments_whenOneTransactionFails_continuesWithNext() {

        Transaction transaction1 = mock(Transaction.class);
        Transaction transaction2 = mock(Transaction.class);

        when(transaction1.getTransactionId()).thenReturn(1);
        when(transaction2.getTransactionId()).thenReturn(2);

        when(transactionRepository
                .findByStatusAndIsUrgentAndIsScheduled(
                        "PENDING_EXECUTION",
                        "NO",
                        "NO"))
                .thenReturn(List.of(transaction1, transaction2));

        doThrow(new RuntimeException("Test execution error"))
                .when(transactionService)
                .executeTransaction(1);

        assertDoesNotThrow(() ->
                paymentJob.processStandardPayments()
        );

        verify(transactionService).executeTransaction(1);

        // Important: eroarea primei tranzactii nu trebuie
        // sa opreasca procesarea urmatoarei.
        verify(transactionService).executeTransaction(2);
    }
}