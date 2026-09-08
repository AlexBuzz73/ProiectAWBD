package com.example.transactionservice;

import com.example.transactionservice.client.AccountClient;
import com.example.transactionservice.client.AccountFeignClient;
import com.example.transactionservice.client.FeignAuthInterceptor;
import com.example.transactionservice.domain.Transaction;
import com.example.transactionservice.dto.AccountInternalSummaryDTO;
import com.example.transactionservice.dto.DebitRequestDTO;
import com.example.transactionservice.dto.OwnAccountTransferDTO;
import com.example.transactionservice.exceptions.ResourceNotFoundException;
import com.example.transactionservice.exceptions.ServiceUnavailableException;
import com.example.transactionservice.repositories.TransactionRepository;
import com.example.transactionservice.services.TransactionService;
import feign.Request;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestJwtConfig.class, TestTokenGenerator.class})
@Transactional
class AccountServiceResilienceTest {

    @Autowired
    private AccountClient accountClient;

    @MockitoBean
    private AccountFeignClient accountFeignClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private RetryRegistry retryRegistry;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestTokenGenerator tokenGenerator;

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("accountServiceCircuitBreaker");
        circuitBreaker.reset();
        reset(accountFeignClient);
    }

    @Test
    @DisplayName("11. Healthy account-service -> getAccount success and circuit stays CLOSED")
    void testHealthyAccountCall() {
        AccountInternalSummaryDTO mockAcc = new AccountInternalSummaryDTO(
                1L, "RO11BANK0000000000000001", "Main", "RON", new BigDecimal("1000.00"), "ACTIVE"
        );
        when(accountFeignClient.getAccount(1L)).thenReturn(mockAcc);

        AccountInternalSummaryDTO res = accountClient.getAccount(1L);
        assertThat(res).isNotNull();
        assertThat(res.getAccountId()).isEqualTo(1L);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("12. Transient failure on GET account -> Retry succeeds on 2nd attempt")
    void testTransientFailure_RetrySuccess() {
        AccountInternalSummaryDTO mockAcc = new AccountInternalSummaryDTO(
                2L, "RO11BANK0000000000000002", "EUR", "EUR", new BigDecimal("500.00"), "ACTIVE"
        );
        when(accountFeignClient.getAccount(2L))
                .thenThrow(new ServiceUnavailableException("Account service network glitch"))
                .thenReturn(mockAcc);

        AccountInternalSummaryDTO res = accountClient.getAccount(2L);
        assertThat(res).isNotNull();
        assertThat(res.getAccountId()).isEqualTo(2L);
        verify(accountFeignClient, times(2)).getAccount(2L);
    }

    @Test
    @DisplayName("13 & 14. Multiple infrastructure failures -> Circuit Breaker OPEN -> Fail fast")
    void testMultipleFailures_CircuitBreakerOpen_FailFast() {
        when(accountFeignClient.getAccount(anyLong()))
                .thenThrow(new ServiceUnavailableException("Account service down"));

        for (int i = 0; i < 5; i++) {
            final long id = 10L + i;
            assertThatThrownBy(() -> accountClient.getAccount(id))
                    .isInstanceOf(ServiceUnavailableException.class);
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        reset(accountFeignClient);
        assertThatThrownBy(() -> accountClient.getAccount(999L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Serviciul de conturi nu este disponibil");

        verifyNoInteractions(accountFeignClient);
    }

    @Test
    @DisplayName("15. Fallback returns controlled HTTP 503 with Correlation ID")
    void testControlled503Fallback() throws Exception {
        when(accountFeignClient.getAccount(88L))
                .thenThrow(new ServiceUnavailableException("Account service cluster unreachable"));

        mockMvc.perform(get("/api/internal/transactions/feign-test/account/88")
                        .header("X-Correlation-Id", "corr-tx-resilience-456"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("Serviciul de conturi nu este disponibil momentan."))
                .andExpect(jsonPath("$.correlationId").value("corr-tx-resilience-456"));
    }

    @Test
    @DisplayName("16 & 17. When account-service is DOWN -> No partial transaction created, status is NOT EXECUTED")
    void testAccountServiceDown_NoPartialTransactionCreated() {
        when(accountFeignClient.getAccount(anyLong()))
                .thenThrow(new ServiceUnavailableException("Account service down"));

        OwnAccountTransferDTO dto = new OwnAccountTransferDTO();
        dto.setSourceAccountId(1L);
        dto.setDestinationAccountId(2L);
        dto.setAmount(new BigDecimal("100.00"));

        long txCountBefore = transactionRepository.count();

        assertThatThrownBy(() -> transactionService.transferBetweenOwnAccounts(dto, 1))
                .isInstanceOf(ServiceUnavailableException.class);

        long txCountAfter = transactionRepository.count();
        assertThat(txCountAfter).isEqualTo(txCountBefore);

        List<Transaction> executedTxs = transactionRepository.findAll().stream()
                .filter(t -> "EXECUTED".equals(t.getStatus()))
                .toList();
        assertThat(executedTxs).noneMatch(t -> t.getAmount().compareTo(new BigDecimal("100.00")) == 0);
    }

    @Test
    @DisplayName("18. Financial writes (debit/credit) are NEVER automatically retried")
    void testFinancialWrites_NeverRetried() {
        when(accountFeignClient.debit(eq(1L), any(DebitRequestDTO.class)))
                .thenThrow(new ServiceUnavailableException("Timeout on debit"));

        assertThatThrownBy(() -> accountClient.debit(1L, new BigDecimal("50.00"), "OP-SAFE-1"))
                .isInstanceOf(ServiceUnavailableException.class);

        // Must be called EXACTLY 1 time - ZERO automatic retries!
        verify(accountFeignClient, times(1)).debit(eq(1L), any(DebitRequestDTO.class));
    }

    @Test
    @DisplayName("19. Business 400 Bad Request is NOT retried")
    void testBusiness400_NotRetried() {
        when(accountFeignClient.debit(eq(1L), any(DebitRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Sold insuficient"));

        assertThatThrownBy(() -> accountClient.debit(1L, new BigDecimal("9999.00"), "OP-SAFE-2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Sold insuficient");

        verify(accountFeignClient, times(1)).debit(eq(1L), any(DebitRequestDTO.class));
    }

    @Test
    @DisplayName("20. Business 403 Access Denied is NOT retried")
    void testBusiness403_NotRetried() {
        when(accountFeignClient.checkAccess(1L, 99, "CO_OWNER"))
                .thenThrow(new AccessDeniedException("Forbidden"));

        boolean allowed = accountClient.checkAccess(1L, 99, "CO_OWNER");
        assertThat(allowed).isFalse();

        verify(accountFeignClient, times(1)).checkAccess(1L, 99, "CO_OWNER");
    }

    @Test
    @DisplayName("21. JWT propagation remains functional with FeignAuthInterceptor")
    void testJwtPropagation() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer test-tx-resilience-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        FeignAuthInterceptor interceptor = new FeignAuthInterceptor();
        feign.RequestTemplate template = new feign.RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers().get(HttpHeaders.AUTHORIZATION))
                .isNotNull()
                .contains("Bearer test-tx-resilience-token");
    }
}
