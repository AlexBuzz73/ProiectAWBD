package com.example.demo.services;

import com.example.demo.domain.AccountAccess;
import com.example.demo.repositories.AccountAccessRepository;
import com.example.demo.repositories.AccountRepository;
import com.example.demo.repositories.CardRepository;
import com.example.demo.repositories.CategoryRepository;
import com.example.demo.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Authorization at the HTTP boundary; callers supply the identity resolved by CurrentUserService. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResourceAuthorizationService {
    public enum AccountPermission { READ, PAY, OWNER }

    private final AccountRepository accounts;
    private final AccountAccessRepository accesses;
    private final CardRepository cards;
    private final CategoryRepository categories;

    public void requireAccount(Integer userId, Long accountId, AccountPermission permission) {
        if (!accounts.existsById(accountId)) throw new ResourceNotFoundException();
        AccountAccess access = accesses.findByAccountAccountIdAndUserUserIdAndStatus(accountId, userId, "ACTIVE")
                .orElseThrow(() -> new AccessDeniedException("Acces interzis la cont"));
        String role = access.getAccessRole();
        boolean allowed = "OWNER".equals(role)
                || (permission != AccountPermission.OWNER && "CO_OWNER".equals(role))
                || (permission == AccountPermission.READ && "VIEWER".equals(role));
        if (!allowed) throw new AccessDeniedException("Rol insuficient pentru aceasta operatie");
    }

    public void requireCard(Long accountId, Integer cardId) {
        var card = cards.findById(cardId.longValue()).orElseThrow(ResourceNotFoundException::new);
        if (!card.getAccount().getAccountId().equals(accountId)) {
            throw new AccessDeniedException("Cardul nu apartine contului");
        }
    }

    public void requireCategory(Integer userId, Integer categoryId, boolean write) {
        var category = categories.findById(categoryId).orElseThrow(ResourceNotFoundException::new);
        boolean system = "Y".equals(category.getIsSystem());
        boolean owner = category.getCreatedByUser() != null
                && userId.equals(category.getCreatedByUser().getUserId());
        if ((write && (system || !owner)) || (!write && !system && !owner)) {
            throw new AccessDeniedException("Acces interzis la categorie");
        }
        if (!"ACTIVE".equals(category.getStatus())) throw new ResourceNotFoundException();
    }
}
