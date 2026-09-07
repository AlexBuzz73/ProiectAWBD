package com.example.transactionservice.services;

import com.example.transactionservice.client.AccountClient;
import com.example.transactionservice.domain.Category;
import com.example.transactionservice.exceptions.ResourceNotFoundException;
import com.example.transactionservice.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResourceAuthorizationService {

    public enum AccountPermission { READ, PAY, OWNER }

    private final AccountClient accountClient;
    private final CategoryRepository categoryRepository;

    public void requireAccount(Integer userId, Long accountId, AccountPermission permission) {
        if (accountId == null) {
            throw new IllegalArgumentException("ID-ul contului nu poate fi nul");
        }
        var account = accountClient.getAccount(accountId);
        if (account == null) {
            throw new ResourceNotFoundException("Contul nu a fost gasit");
        }

        String requiredRole = switch (permission) {
            case OWNER -> "OWNER";
            case PAY -> "CO_OWNER";
            case READ -> "VIEWER";
        };

        boolean allowed = accountClient.checkAccess(accountId, userId, requiredRole);
        if (!allowed) {
            throw new AccessDeniedException("Acces interzis la cont");
        }
    }

    public void requireCategory(Integer userId, Integer categoryId, boolean write) {
        if (categoryId == null) {
            throw new IllegalArgumentException("ID-ul categoriei nu poate fi nul");
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria nu a fost gasita"));

        boolean system = "Y".equals(category.getIsSystem());
        boolean owner = category.getCreatedByUserId() != null && userId.equals(category.getCreatedByUserId());

        if ((write && (system || !owner)) || (!write && !system && !owner)) {
            throw new AccessDeniedException("Acces interzis la categorie");
        }

        if (!"ACTIVE".equals(category.getStatus())) {
            throw new ResourceNotFoundException("Categoria nu este activa");
        }
    }
}
