package com.example.transactionservice.client;

import com.example.transactionservice.dto.*;
import com.example.transactionservice.exceptions.ResourceNotFoundException;
import com.example.transactionservice.services.CurrentUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class AccountClient {

    private final RestClient restClient;
    private final CurrentUserService currentUserService;

    @Autowired
    public AccountClient(@Value("${account-service.url:http://localhost:8082}") String accountServiceUrl,
                         CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
        this.restClient = RestClient.builder().baseUrl(accountServiceUrl).build();
    }

    public AccountClient(RestClient restClient, CurrentUserService currentUserService) {
        this.restClient = restClient;
        this.currentUserService = currentUserService;
    }

    private RestClient.RequestHeadersSpec<?> withAuth(RestClient.RequestHeadersSpec<?> spec) {
        String token = currentUserService.getJwtTokenValue();
        if (token != null && !token.isBlank()) {
            return spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return spec;
    }

    public AccountInternalSummaryDTO getAccount(Long accountId) {
        try {
            return withAuth(restClient.get().uri("/api/internal/accounts/{id}", accountId))
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new ResourceNotFoundException("Contul sursă nu a fost găsit.");
                    })
                    .body(AccountInternalSummaryDTO.class);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching account {}: {}", accountId, e.getMessage());
            throw new IllegalArgumentException("Contul nu a fost găsit sau a apărut o eroare: " + e.getMessage());
        }
    }

    public AccountInternalSummaryDTO getAccountByIban(String iban) {
        try {
            return withAuth(restClient.get().uri("/api/internal/accounts/by-iban/{iban}", iban))
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new ResourceNotFoundException("Contul nu a fost găsit pentru IBAN: " + iban);
                    })
                    .body(AccountInternalSummaryDTO.class);
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
            return withAuth(restClient.post().uri("/api/internal/accounts/{id}/debit", accountId).body(dto))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new IllegalArgumentException("Eroare la debitare cont: " + res.getStatusCode());
                    })
                    .body(AccountInternalSummaryDTO.class);
        } catch (Exception e) {
            log.error("Debit failed for account {}: {}", accountId, e.getMessage());
            throw new IllegalArgumentException("Debit esuat: " + e.getMessage());
        }
    }

    public AccountInternalSummaryDTO credit(Long accountId, BigDecimal amount, String operationId) {
        CreditRequestDTO dto = new CreditRequestDTO(amount, operationId);
        try {
            return withAuth(restClient.post().uri("/api/internal/accounts/{id}/credit", accountId).body(dto))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new IllegalArgumentException("Eroare la creditare cont: " + res.getStatusCode());
                    })
                    .body(AccountInternalSummaryDTO.class);
        } catch (Exception e) {
            log.error("Credit failed for account {}: {}", accountId, e.getMessage());
            throw new IllegalArgumentException("Credit esuat: " + e.getMessage());
        }
    }

    public boolean checkAccess(Long accountId, Integer userId, String requiredRole) {
        try {
            Boolean allowed = withAuth(restClient.get().uri(uriBuilder -> uriBuilder
                    .path("/api/internal/accounts/{id}/access-check")
                    .queryParam("userId", userId)
                    .queryParam("requiredRole", requiredRole != null ? requiredRole : "")
                    .build(accountId)))
                    .retrieve()
                    .body(Boolean.class);
            return Boolean.TRUE.equals(allowed);
        } catch (Exception e) {
            log.error("Access check failed for account {} userId {}: {}", accountId, userId, e.getMessage());
            return false;
        }
    }

    public UserLimitResponseDTO getUserLimits(Integer userId) {
        try {
            return withAuth(restClient.get().uri("/api/internal/accounts/limits/user/{userId}", userId))
                    .retrieve()
                    .body(UserLimitResponseDTO.class);
        } catch (Exception e) {
            log.warn("Could not retrieve user limits for userId {}, using fallback: {}", userId, e.getMessage());
            UserLimitResponseDTO fallback = new UserLimitResponseDTO();
            fallback.setMaxAmountPerTransactionRon(new BigDecimal("1000000.00"));
            return fallback;
        }
    }

    public List<AccountInternalSummaryDTO> getUserAccounts(Integer userId) {
        try {
            List<AccountInternalSummaryDTO> list = withAuth(restClient.get().uri("/api/internal/accounts/user/{userId}", userId))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<AccountInternalSummaryDTO>>() {});
            return list != null ? list : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching user accounts for userId {}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
