package com.example.accountservice.services;

import com.example.accountservice.domain.AccountAccess;
import com.example.accountservice.domain.Card;
import com.example.accountservice.exceptions.ResourceNotFoundException;
import com.example.accountservice.repositories.AccountAccessRepository;
import com.example.accountservice.repositories.AccountRepository;
import com.example.accountservice.repositories.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResourceAuthorizationService {

    public enum AccountPermission {
        READ,
        OPERATIONAL,
        OWNER
    }

    private final AccountRepository accountRepository;
    private final AccountAccessRepository accountAccessRepository;
    private final CardRepository cardRepository;

    public void requireAccount(Integer userId, Long accountId, AccountPermission permission) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Contul cu id " + accountId + " nu a fost gasit");
        }

        AccountAccess access = accountAccessRepository
                .findByAccountAccountIdAndUserIdAndStatus(accountId, userId, "ACTIVE")
                .orElseThrow(() -> new AccessDeniedException("Acces interzis la cont"));

        String role = access.getAccessRole();
        boolean allowed = "OWNER".equalsIgnoreCase(role)
                || (permission != AccountPermission.OWNER && "CO_OWNER".equalsIgnoreCase(role))
                || (permission == AccountPermission.READ && "VIEWER".equalsIgnoreCase(role));

        if (!allowed) {
            throw new AccessDeniedException("Rol insuficient (" + role + ") pentru aceasta operatie");
        }
    }

    public void requireCard(Long accountId, Integer cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Cardul cu id " + cardId + " nu a fost gasit"));

        if (!card.getAccount().getAccountId().equals(accountId)) {
            throw new AccessDeniedException("Cardul nu apartine acestui cont");
        }
    }
}
