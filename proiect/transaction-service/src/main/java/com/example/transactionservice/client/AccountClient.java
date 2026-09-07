package com.example.transactionservice.client;

import com.example.transactionservice.dto.*;
import com.example.transactionservice.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountClient {

    private final AccountFeignClient accountFeignClient;

    public AccountInternalSummaryDTO getAccount(Long accountId) {
        try {
            AccountInternalSummaryDTO summary = accountFeignClient.getAccount(accountId);
            if (summary == null) {
                throw new ResourceNotFoundException("Contul sursă nu a fost găsit.");
            }
            return summary;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching account {}: {}", accountId, e.getMessage());
            throw new IllegalArgumentException("Contul nu a fost găsit sau a apărut o eroare: " + e.getMessage());
        }
    }

    public AccountInternalSummaryDTO getAccountByIban(String iban) {
        try {
            return accountFeignClient.getAccountByIban(iban);
        } catch (ResourceNotFoundException e) {
            return null;
        } catch (Exception e) {
            log.warn("Account not found or error for IBAN {}: {}", iban, e.getMessage());
            return null;
        }
    }

    public AccountInternalSummaryDTO debit(Long accountId, BigDecimal amount, String operationId) {
        DebitRequestDTO dto = new DebitRequestDTO(amount, operationId);
        try {
            return accountFeignClient.debit(accountId, dto);
        } catch (Exception e) {
            log.error("Debit failed for account {}: {}", accountId, e.getMessage());
            throw new IllegalArgumentException("Debit esuat: " + e.getMessage());
        }
    }

    public AccountInternalSummaryDTO credit(Long accountId, BigDecimal amount, String operationId) {
        CreditRequestDTO dto = new CreditRequestDTO(amount, operationId);
        try {
            return accountFeignClient.credit(accountId, dto);
        } catch (Exception e) {
            log.error("Credit failed for account {}: {}", accountId, e.getMessage());
            throw new IllegalArgumentException("Credit esuat: " + e.getMessage());
        }
    }

    public boolean checkAccess(Long accountId, Integer userId, String requiredRole) {
        try {
            Boolean allowed = accountFeignClient.checkAccess(accountId, userId, requiredRole != null ? requiredRole : "");
            return Boolean.TRUE.equals(allowed);
        } catch (Exception e) {
            log.error("Access check failed for account {} userId {}: {}", accountId, userId, e.getMessage());
            return false;
        }
    }

    public UserLimitResponseDTO getUserLimits(Integer userId) {
        try {
            UserLimitResponseDTO limits = accountFeignClient.getUserLimits(userId);
            return limits != null ? limits : defaultLimits();
        } catch (Exception e) {
            log.warn("Could not retrieve user limits for userId {}, using fallback: {}", userId, e.getMessage());
            return defaultLimits();
        }
    }

    private UserLimitResponseDTO defaultLimits() {
        UserLimitResponseDTO fallback = new UserLimitResponseDTO();
        fallback.setMaxAmountPerTransactionRon(new BigDecimal("1000000.00"));
        return fallback;
    }

    public List<AccountInternalSummaryDTO> getUserAccounts(Integer userId) {
        try {
            List<AccountInternalSummaryDTO> list = accountFeignClient.getUserAccounts(userId);
            return list != null ? list : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching user accounts for userId {}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
