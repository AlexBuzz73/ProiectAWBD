package com.example.demo.mappers;

import com.example.demo.domain.Transaction;
import com.example.demo.dto.TransactionSummaryDTO;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    /** A counterparty may see the payment, but not another user's private labels. */
    public TransactionSummaryDTO toTransactionSummaryDTO(Transaction transaction, Integer viewerId) {
        TransactionSummaryDTO dto = toTransactionSummaryDTO(transaction);
        var category = transaction.getCategory();
        if (category != null && !"Y".equals(category.getIsSystem())
                && (category.getCreatedByUser() == null
                || !viewerId.equals(category.getCreatedByUser().getUserId()))) {
            dto.setCategoryId(null);
            dto.setCategoryName(null);
        }
        if (!canReadAccount(transaction.getSourceAccount(), viewerId)) {
            dto.setSourceAccountId(null);
            dto.setSourceAccountAlias(null);
        }
        if (!canReadAccount(transaction.getDestinationAccount(), viewerId)) {
            dto.setDestinationAccountId(null);
            dto.setDestinationAccountAlias(null);
        }
        return dto;
    }

    private boolean canReadAccount(com.example.demo.domain.Account account, Integer viewerId) {
        return account != null && account.getAccountAccessList() != null
                && account.getAccountAccessList().stream().anyMatch(access ->
                viewerId.equals(access.getUser().getUserId()) && "ACTIVE".equals(access.getStatus())
                        && java.util.Set.of("OWNER", "CO_OWNER", "VIEWER").contains(
                                access.getAccessRole() == null ? "" : access.getAccessRole()));
    }

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
