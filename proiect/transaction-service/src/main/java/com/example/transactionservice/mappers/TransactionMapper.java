package com.example.transactionservice.mappers;

import com.example.transactionservice.domain.Transaction;
import com.example.transactionservice.dto.TransactionSummaryDTO;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TransactionMapper {

    public TransactionSummaryDTO toTransactionSummaryDTO(Transaction transaction) {
        if (transaction == null) return null;

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
                transaction.getSourceAccountId(),
                transaction.getSourceAccountAlias(),
                transaction.getSourceAccountIban(),
                transaction.getDestinationAccountId(),
                transaction.getDestinationAccountAlias(),
                transaction.getDestinationAccountIban(),
                transaction.getDestinationIban(),
                categoryId,
                categoryName
        );
    }

    public TransactionSummaryDTO toTransactionSummaryDTO(Transaction transaction, Integer viewerId) {
        TransactionSummaryDTO dto = toTransactionSummaryDTO(transaction);
        if (dto == null) return null;

        var category = transaction.getCategory();
        if (category != null && !"Y".equals(category.getIsSystem())
                && (category.getCreatedByUserId() == null || !viewerId.equals(category.getCreatedByUserId()))) {
            dto.setCategoryId(null);
            dto.setCategoryName(null);
        }

        return dto;
    }

    public TransactionSummaryDTO toTransactionSummaryDTO(Transaction transaction, Integer viewerId, Set<Long> userAccountIds) {
        TransactionSummaryDTO dto = toTransactionSummaryDTO(transaction, viewerId);
        if (dto == null) return null;

        if (userAccountIds != null) {
            if (transaction.getSourceAccountId() != null && !userAccountIds.contains(transaction.getSourceAccountId())) {
                dto.setSourceAccountId(null);
                dto.setSourceAccountAlias(null);
            }
            if (transaction.getDestinationAccountId() != null && !userAccountIds.contains(transaction.getDestinationAccountId())) {
                dto.setDestinationAccountId(null);
                dto.setDestinationAccountAlias(null);
            }
        }
        return dto;
    }
}
