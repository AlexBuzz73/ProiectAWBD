package com.example.transactionservice;

import com.example.transactionservice.client.AccountClient;
import com.example.transactionservice.client.AccountFeignClient;
import com.example.transactionservice.client.CustomFeignErrorDecoder;
import com.example.transactionservice.client.FeignAuthInterceptor;
import com.example.transactionservice.dto.AccountInternalSummaryDTO;
import com.example.transactionservice.dto.CreditRequestDTO;
import com.example.transactionservice.dto.DebitRequestDTO;
import com.example.transactionservice.dto.UserLimitResponseDTO;
import com.example.transactionservice.exceptions.ResourceNotFoundException;
import com.example.transactionservice.exceptions.ServiceUnavailableException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountClientTest {

    @Mock
    private AccountFeignClient accountFeignClient;

    private AccountClient accountClient;
    private FeignAuthInterceptor authInterceptor;
    private CustomFeignErrorDecoder errorDecoder;

    @BeforeEach
    void setUp() {
        accountClient = new AccountClient(accountFeignClient);
        authInterceptor = new FeignAuthInterceptor();
        errorDecoder = new CustomFeignErrorDecoder();
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getAccount returns AccountInternalSummaryDTO on 200 OK")
    void testGetAccountSuccess() {
        AccountInternalSummaryDTO expected = new AccountInternalSummaryDTO(
                1L, "RO11BANK0000000000000001", "Main RON", "RON", new BigDecimal("1500.00"), "ACTIVE"
        );
        when(accountFeignClient.getAccount(1L)).thenReturn(expected);

        AccountInternalSummaryDTO result = accountClient.getAccount(1L);
        assertThat(result).isNotNull();
        assertThat(result.getAccountId()).isEqualTo(1L);
        assertThat(result.getBalance()).isEqualByComparingTo("1500.00");
    }

    @Test
    @DisplayName("getAccount throws ResourceNotFoundException on 404")
    void testGetAccountNotFound() {
        when(accountFeignClient.getAccount(999L)).thenThrow(new ResourceNotFoundException("Not found"));

        assertThatThrownBy(() -> accountClient.getAccount(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getAccountByIban returns AccountInternalSummaryDTO on 200 OK")
    void testGetAccountByIbanSuccess() {
        AccountInternalSummaryDTO expected = new AccountInternalSummaryDTO(
                2L, "RO11BANK0000000000000002", "Dest EUR", "EUR", new BigDecimal("500.00"), "ACTIVE"
        );
        when(accountFeignClient.getAccountByIban("RO11BANK0000000000000002")).thenReturn(expected);

        AccountInternalSummaryDTO result = accountClient.getAccountByIban("RO11BANK0000000000000002");
        assertThat(result).isNotNull();
        assertThat(result.getIban()).isEqualTo("RO11BANK0000000000000002");
    }

    @Test
    @DisplayName("getAccountByIban returns null on ResourceNotFoundException")
    void testGetAccountByIbanNotFound() {
        when(accountFeignClient.getAccountByIban("RO99UNKNOWN")).thenThrow(new ResourceNotFoundException("Not found"));

        AccountInternalSummaryDTO result = accountClient.getAccountByIban("RO99UNKNOWN");
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("debit returns updated summary on 200 OK")
    void testDebitSuccess() {
        AccountInternalSummaryDTO expected = new AccountInternalSummaryDTO(
                1L, "RO11BANK0000000000000001", "Main RON", "RON", new BigDecimal("1400.00"), "ACTIVE"
        );
        when(accountFeignClient.debit(eq(1L), any(DebitRequestDTO.class))).thenReturn(expected);

        AccountInternalSummaryDTO result = accountClient.debit(1L, new BigDecimal("100.00"), "OP-1");
        assertThat(result).isNotNull();
        assertThat(result.getBalance()).isEqualByComparingTo("1400.00");
    }

    @Test
    @DisplayName("debit throws IllegalArgumentException on failure")
    void testDebitFailure() {
        when(accountFeignClient.debit(eq(1L), any(DebitRequestDTO.class))).thenThrow(new RuntimeException("Balance too low"));

        assertThatThrownBy(() -> accountClient.debit(1L, new BigDecimal("99999.00"), "OP-FAIL"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("credit returns updated summary on 200 OK")
    void testCreditSuccess() {
        AccountInternalSummaryDTO expected = new AccountInternalSummaryDTO(
                2L, "RO11BANK0000000000000002", "Dest EUR", "EUR", new BigDecimal("600.00"), "ACTIVE"
        );
        when(accountFeignClient.credit(eq(2L), any(CreditRequestDTO.class))).thenReturn(expected);

        AccountInternalSummaryDTO result = accountClient.credit(2L, new BigDecimal("100.00"), "OP-2");
        assertThat(result).isNotNull();
        assertThat(result.getBalance()).isEqualByComparingTo("600.00");
    }

    @Test
    @DisplayName("checkAccess returns true when access is granted")
    void testCheckAccessSuccess() {
        when(accountFeignClient.checkAccess(1L, 10, "CO_OWNER")).thenReturn(true);

        boolean allowed = accountClient.checkAccess(1L, 10, "CO_OWNER");
        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("getUserLimits returns limits on 200 OK")
    void testGetUserLimitsSuccess() {
        UserLimitResponseDTO expected = new UserLimitResponseDTO(1, new BigDecimal("5000.00"), new BigDecimal("20000.00"), new BigDecimal("10"), "ACTIVE");
        when(accountFeignClient.getUserLimits(10)).thenReturn(expected);

        UserLimitResponseDTO result = accountClient.getUserLimits(10);
        assertThat(result).isNotNull();
        assertThat(result.getMaxAmountPerTransactionRon()).isEqualByComparingTo("5000.00");
    }

    @Test
    @DisplayName("getUserAccounts returns list on 200 OK")
    void testGetUserAccountsSuccess() {
        List<AccountInternalSummaryDTO> expected = List.of(
                new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "RON acc", "RON", new BigDecimal("1000.00"), "ACTIVE")
        );
        when(accountFeignClient.getUserAccounts(10)).thenReturn(expected);

        List<AccountInternalSummaryDTO> result = accountClient.getUserAccounts(10);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccountId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("FeignAuthInterceptor propagates Authorization from current HttpServletRequest")
    void testFeignAuthInterceptorFromHttpRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer test-http-token-account");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestTemplate template = new RequestTemplate();
        authInterceptor.apply(template);

        Collection<String> authHeaders = template.headers().get(HttpHeaders.AUTHORIZATION);
        assertThat(authHeaders).isNotNull().contains("Bearer test-http-token-account");
    }

    @Test
    @DisplayName("FeignAuthInterceptor propagates Bearer token from SecurityContext fallback")
    void testFeignAuthInterceptorFromSecurityContext() {
        Jwt jwt = Jwt.withTokenValue("jwt-security-token-feign")
                .header("alg", "RS256")
                .claim("sub", "user@test.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        JwtAuthenticationToken authToken = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(authToken);

        RequestTemplate template = new RequestTemplate();
        authInterceptor.apply(template);

        Collection<String> authHeaders = template.headers().get(HttpHeaders.AUTHORIZATION);
        assertThat(authHeaders).isNotNull().contains("Bearer jwt-security-token-feign");
    }

    @Test
    @DisplayName("CustomFeignErrorDecoder maps HTTP 404 to ResourceNotFoundException")
    void testErrorDecoder404() {
        Response response = Response.builder()
                .status(404)
                .reason("Not Found")
                .request(Request.create(Request.HttpMethod.GET, "/api/internal/accounts/1", Collections.emptyMap(), null, StandardCharsets.UTF_8, null))
                .headers(Collections.emptyMap())
                .build();

        Exception ex = errorDecoder.decode("getAccount", response);
        assertThat(ex).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("CustomFeignErrorDecoder maps HTTP 403 to AccessDeniedException")
    void testErrorDecoder403() {
        Response response = Response.builder()
                .status(403)
                .reason("Forbidden")
                .request(Request.create(Request.HttpMethod.GET, "/api/internal/accounts/1", Collections.emptyMap(), null, StandardCharsets.UTF_8, null))
                .headers(Collections.emptyMap())
                .build();

        Exception ex = errorDecoder.decode("getAccount", response);
        assertThat(ex).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("CustomFeignErrorDecoder maps HTTP 400 to IllegalArgumentException")
    void testErrorDecoder400() {
        Response response = Response.builder()
                .status(400)
                .reason("Bad Request")
                .request(Request.create(Request.HttpMethod.GET, "/api/internal/accounts/1", Collections.emptyMap(), null, StandardCharsets.UTF_8, null))
                .headers(Collections.emptyMap())
                .build();

        Exception ex = errorDecoder.decode("getAccount", response);
        assertThat(ex).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("CustomFeignErrorDecoder maps HTTP 503 to ServiceUnavailableException")
    void testErrorDecoder503() {
        Response response = Response.builder()
                .status(503)
                .reason("Service Unavailable")
                .request(Request.create(Request.HttpMethod.GET, "/api/internal/accounts/1", Collections.emptyMap(), null, StandardCharsets.UTF_8, null))
                .headers(Collections.emptyMap())
                .build();

        Exception ex = errorDecoder.decode("getAccount", response);
        assertThat(ex).isInstanceOf(ServiceUnavailableException.class);
    }
}
