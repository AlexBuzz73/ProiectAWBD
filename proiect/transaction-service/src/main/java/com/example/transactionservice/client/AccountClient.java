package com.example.transactionservice.client;

import com.example.transactionservice.dto.*;
import com.example.transactionservice.exceptions.ResourceNotFoundException;
import com.example.transactionservice.exceptions.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountClient {

    private final AccountFeignClient accountFeignClient;

    // SAFE READ - Protected by Circuit Breaker & Retry
    @CircuitBreaker(name = "accountServiceCircuitBreaker", fallbackMethod = "getAccountFallback")
    @Retry(name = "accountServiceRetry")
    public AccountInternalSummaryDTO getAccount(Long accountId) {
        AccountInternalSummaryDTO summary = accountFeignClient.getAccount(accountId);
        if (summary == null) {
            throw new ResourceNotFoundException("Contul sursă nu a fost găsit.");
        }
        return summary;
    }

    public AccountInternalSummaryDTO getAccountFallback(Long accountId, Throwable t) {
        log.error("Resilience fallback for getAccount({}): {}", accountId, t.getMessage());
        if (t instanceof ResourceNotFoundException rnfe) {
            throw rnfe;
        }
        if (t instanceof AccessDeniedException ade) {
            throw ade;
        }
        throw new ServiceUnavailableException("Serviciul de conturi nu este disponibil momentan.");
    }

    // SAFE READ - Protected by Circuit Breaker & Retry
    @CircuitBreaker(name = "accountServiceCircuitBreaker", fallbackMethod = "getAccountByIbanFallback")
    @Retry(name = "accountServiceRetry")
    public AccountInternalSummaryDTO getAccountByIban(String iban) {
        try {
            return accountFeignClient.getAccountByIban(iban);
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }

    public AccountInternalSummaryDTO getAccountByIbanFallback(String iban, Throwable t) {
        log.error("Resilience fallback for getAccountByIban({}): {}", iban, t.getMessage());
        if (t instanceof ResourceNotFoundException) {
            return null;
        }
        throw new ServiceUnavailableException("Serviciul de conturi nu este disponibil momentan.");
    }

    // FINANCIAL MUTATION - CRITICAL: NEVER RETRY AUTOMATICALLY (Circuit Breaker ONLY, NO @Retry)
    @CircuitBreaker(name = "accountServiceCircuitBreaker", fallbackMethod = "debitFallback")
    public AccountInternalSummaryDTO debit(Long accountId, BigDecimal amount, String operationId) {
        DebitRequestDTO dto = new DebitRequestDTO(amount, operationId);
        return accountFeignClient.debit(accountId, dto);
    }

    public AccountInternalSummaryDTO debitFallback(Long accountId, BigDecimal amount, String operationId, Throwable t) {
        log.error("Resilience fallback for debit(acc={}, amount={}, op={}): {}", accountId, amount, operationId, t.getMessage());
        if (t instanceof IllegalArgumentException iae) {
            throw iae;
        }
        if (t instanceof ResourceNotFoundException rnfe) {
            throw rnfe;
        }
        if (t instanceof AccessDeniedException ade) {
            throw ade;
        }
        throw new ServiceUnavailableException("Serviciul de conturi nu este disponibil pentru debitare.");
    }

    // FINANCIAL MUTATION - CRITICAL: NEVER RETRY AUTOMATICALLY (Circuit Breaker ONLY, NO @Retry)
    @CircuitBreaker(name = "accountServiceCircuitBreaker", fallbackMethod = "creditFallback")
    public AccountInternalSummaryDTO credit(Long accountId, BigDecimal amount, String operationId) {
        CreditRequestDTO dto = new CreditRequestDTO(amount, operationId);
        return accountFeignClient.credit(accountId, dto);
    }

    public AccountInternalSummaryDTO creditFallback(Long accountId, BigDecimal amount, String operationId, Throwable t) {
        log.error("Resilience fallback for credit(acc={}, amount={}, op={}): {}", accountId, amount, operationId, t.getMessage());
        if (t instanceof IllegalArgumentException iae) {
            throw iae;
        }
        if (t instanceof ResourceNotFoundException rnfe) {
            throw rnfe;
        }
        if (t instanceof AccessDeniedException ade) {
            throw ade;
        }
        throw new ServiceUnavailableException("Serviciul de conturi nu este disponibil pentru creditare.");
    }

    // SAFE READ - Protected by Circuit Breaker & Retry
    @CircuitBreaker(name = "accountServiceCircuitBreaker", fallbackMethod = "checkAccessFallback")
    @Retry(name = "accountServiceRetry")
    public boolean checkAccess(Long accountId, Integer userId, String requiredRole) {
        Boolean allowed = accountFeignClient.checkAccess(accountId, userId, requiredRole != null ? requiredRole : "");
        return Boolean.TRUE.equals(allowed);
    }

    public boolean checkAccessFallback(Long accountId, Integer userId, String requiredRole, Throwable t) {
        log.error("Resilience fallback for checkAccess(acc={}, user={}): {}", accountId, userId, t.getMessage());
        if (t instanceof AccessDeniedException) {
            return false;
        }
        throw new ServiceUnavailableException("Serviciul de conturi nu este disponibil momentan.");
    }

    // SAFE READ - Protected by Circuit Breaker & Retry
    @CircuitBreaker(name = "accountServiceCircuitBreaker", fallbackMethod = "getUserLimitsFallback")
    @Retry(name = "accountServiceRetry")
    public UserLimitResponseDTO getUserLimits(Integer userId) {
        UserLimitResponseDTO limits = accountFeignClient.getUserLimits(userId);
        return limits != null ? limits : defaultLimits();
    }

    public UserLimitResponseDTO getUserLimitsFallback(Integer userId, Throwable t) {
        log.warn("Resilience fallback for getUserLimits({}): {}", userId, t.getMessage());
        return defaultLimits();
    }

    private UserLimitResponseDTO defaultLimits() {
        UserLimitResponseDTO fallback = new UserLimitResponseDTO();
        fallback.setMaxAmountPerTransactionRon(new BigDecimal("1000000.00"));
        return fallback;
    }

    // SAFE READ - Protected by Circuit Breaker & Retry
    @CircuitBreaker(name = "accountServiceCircuitBreaker", fallbackMethod = "getUserAccountsFallback")
    @Retry(name = "accountServiceRetry")
    public List<AccountInternalSummaryDTO> getUserAccounts(Integer userId) {
        List<AccountInternalSummaryDTO> list = accountFeignClient.getUserAccounts(userId);
        return list != null ? list : Collections.emptyList();
    }

    public List<AccountInternalSummaryDTO> getUserAccountsFallback(Integer userId, Throwable t) {
        log.error("Resilience fallback for getUserAccounts({}): {}", userId, t.getMessage());
        throw new ServiceUnavailableException("Serviciul de conturi nu este disponibil momentan.");
    }

    // SAFE READ - Protected by Circuit Breaker & Retry
    @CircuitBreaker(name = "accountServiceCircuitBreaker", fallbackMethod = "getInstanceInfoFallback")
    @Retry(name = "accountServiceRetry")
    public Map<String, Object> getInstanceInfo() {
        return accountFeignClient.getInstanceInfo();
    }

    public Map<String, Object> getInstanceInfoFallback(Throwable t) {
        log.error("Resilience fallback for getInstanceInfo(): {}", t.getMessage());
        throw new ServiceUnavailableException("Serviciul de conturi nu este disponibil momentan.");
    }
}
