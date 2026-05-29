package com.example.demo.mappers;

import com.example.demo.domain.Transaction;
import com.example.demo.dto.TransactionSummaryDTO;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionSummaryDTO toTransactionSummaryDTO(Transaction transaction) {
        Long sourceAccountId = null;
        String sourceAccountAlias = null;
        String sourceAccountIban = null;

        if (transaction.getSourceAccount() != null) {
            sourceAccountId = transaction.getSourceAccount().getAccountId();
            sourceAccountAlias = transaction.getSourceAccount().getAlias();
            sourceAccountIban = transaction.getSourceAccount().getIban();
        }

        Long destinationAccountId = null;
        String destinationAccountAlias = null;
        String destinationAccountIban = null;

        if (transaction.getDestinationAccount() != null) {
            destinationAccountId = transaction.getDestinationAccount().getAccountId();
            destinationAccountAlias = transaction.getDestinationAccount().getAlias();
            destinationAccountIban = transaction.getDestinationAccount().getIban();
        }

        Integer categoryId = null;
        String categoryName = null;

        if (transaction.getCategory() != null) {
            categoryId = transaction.getCategory().getCategoryId();
            categoryName = transaction.getCategory().getName();
        }

        return new TransactionSummaryDTO(
                transaction.getTransactionId(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getDescription(),
                transaction.getStatus(),
                transaction.getCreatedAt(),
                sourceAccountId,
                sourceAccountAlias,
                sourceAccountIban,
                destinationAccountId,
                destinationAccountAlias,
                destinationAccountIban,
                transaction.getDestinationIban(),
                categoryId,
                categoryName
        );
    }
}