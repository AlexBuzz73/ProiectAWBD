package com.example.transactionservice;

import com.example.transactionservice.client.AccountClient;
import com.example.transactionservice.dto.AccountInternalSummaryDTO;
import com.example.transactionservice.dto.UserLimitResponseDTO;
import com.example.transactionservice.exceptions.ResourceNotFoundException;
import com.example.transactionservice.services.CurrentUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountClientTest {

    private AccountClient accountClient;
    private MockRestServiceServer mockServer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() throws Exception {
        currentUserService = Mockito.mock(CurrentUserService.class);
        Mockito.when(currentUserService.getJwtTokenValue()).thenReturn("test-token");

        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8082");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        accountClient = new AccountClient(restClient, currentUserService);
    }

    @Test
    @DisplayName("getAccount returns AccountInternalSummaryDTO on 200 OK")
    void testGetAccountSuccess() throws Exception {
        AccountInternalSummaryDTO expected = new AccountInternalSummaryDTO(
                1L, "RO11BANK0000000000000001", "Main RON", "RON", new BigDecimal("1500.00"), "ACTIVE"
        );

        mockServer.expect(MockRestRequestMatchers.requestTo("http://localhost:8082/api/internal/accounts/1"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header("Authorization", "Bearer test-token"))
                .andRespond(MockRestResponseCreators.withSuccess(objectMapper.writeValueAsString(expected), MediaType.APPLICATION_JSON));

        AccountInternalSummaryDTO result = accountClient.getAccount(1L);
        assertThat(result).isNotNull();
        assertThat(result.getAccountId()).isEqualTo(1L);
        assertThat(result.getBalance()).isEqualByComparingTo("1500.00");
        mockServer.verify();
    }

    @Test
    @DisplayName("getAccount throws ResourceNotFoundException on 404")
    void testGetAccountNotFound() {
        mockServer.expect(MockRestRequestMatchers.requestTo("http://localhost:8082/api/internal/accounts/999"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> accountClient.getAccount(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        mockServer.verify();
    }

    @Test
    @DisplayName("getAccountByIban returns AccountInternalSummaryDTO on 200 OK")
    void testGetAccountByIbanSuccess() throws Exception {
        AccountInternalSummaryDTO expected = new AccountInternalSummaryDTO(
                2L, "RO11BANK0000000000000002", "Dest EUR", "EUR", new BigDecimal("500.00"), "ACTIVE"
        );

        mockServer.expect(MockRestRequestMatchers.requestTo("http://localhost:8082/api/internal/accounts/by-iban/RO11BANK0000000000000002"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withSuccess(objectMapper.writeValueAsString(expected), MediaType.APPLICATION_JSON));

        AccountInternalSummaryDTO result = accountClient.getAccountByIban("RO11BANK0000000000000002");
        assertThat(result).isNotNull();
        assertThat(result.getIban()).isEqualTo("RO11BANK0000000000000002");
        mockServer.verify();
    }

    @Test
    @DisplayName("getAccountByIban returns null on 404")
    void testGetAccountByIbanNotFound() {
        mockServer.expect(MockRestRequestMatchers.requestTo("http://localhost:8082/api/internal/accounts/by-iban/RO99UNKNOWN"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND));

        AccountInternalSummaryDTO result = accountClient.getAccountByIban("RO99UNKNOWN");
        assertThat(result).isNull();
        mockServer.verify();
    }

    @Test
    @DisplayName("debit returns updated summary on 200 OK")
    void testDebitSuccess() throws Exception {
        AccountInternalSummaryDTO expected = new AccountInternalSummaryDTO(
                1L, "RO11BANK0000000000000001", "Main RON", "RON", new BigDecimal("1400.00"), "ACTIVE"
        );

        mockServer.expect(MockRestRequestMatchers.requestTo("http://localhost:8082/api/internal/accounts/1/debit"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withSuccess(objectMapper.writeValueAsString(expected), MediaType.APPLICATION_JSON));

        AccountInternalSummaryDTO result = accountClient.debit(1L, new BigDecimal("100.00"), "OP-1");
        assertThat(result).isNotNull();
        assertThat(result.getBalance()).isEqualByComparingTo("1400.00");
        mockServer.verify();
    }

    @Test
    @DisplayName("debit throws IllegalArgumentException on 400 Bad Request")
    void testDebitFailure() {
        mockServer.expect(MockRestRequestMatchers.requestTo("http://localhost:8082/api/internal/accounts/1/debit"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> accountClient.debit(1L, new BigDecimal("99999.00"), "OP-FAIL"))
                .isInstanceOf(IllegalArgumentException.class);
        mockServer.verify();
    }

    @Test
    @DisplayName("credit returns updated summary on 200 OK")
    void testCreditSuccess() throws Exception {
        AccountInternalSummaryDTO expected = new AccountInternalSummaryDTO(
                2L, "RO11BANK0000000000000002", "Dest EUR", "EUR", new BigDecimal("600.00"), "ACTIVE"
        );

        mockServer.expect(MockRestRequestMatchers.requestTo("http://localhost:8082/api/internal/accounts/2/credit"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withSuccess(objectMapper.writeValueAsString(expected), MediaType.APPLICATION_JSON));

        AccountInternalSummaryDTO result = accountClient.credit(2L, new BigDecimal("100.00"), "OP-2");
        assertThat(result).isNotNull();
        assertThat(result.getBalance()).isEqualByComparingTo("600.00");
        mockServer.verify();
    }

    @Test
    @DisplayName("checkAccess returns true when access is granted")
    void testCheckAccessSuccess() {
        mockServer.expect(MockRestRequestMatchers.requestTo("http://localhost:8082/api/internal/accounts/1/access-check?userId=10&requiredRole=CO_OWNER"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withSuccess("true", MediaType.APPLICATION_JSON));

        boolean allowed = accountClient.checkAccess(1L, 10, "CO_OWNER");
        assertThat(allowed).isTrue();
        mockServer.verify();
    }

    @Test
    @DisplayName("getUserLimits returns limits on 200 OK")
    void testGetUserLimitsSuccess() throws Exception {
        UserLimitResponseDTO expected = new UserLimitResponseDTO(1, new BigDecimal("5000.00"), new BigDecimal("20000.00"), new BigDecimal("10"), "ACTIVE");

        mockServer.expect(MockRestRequestMatchers.requestTo("http://localhost:8082/api/internal/accounts/limits/user/10"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withSuccess(objectMapper.writeValueAsString(expected), MediaType.APPLICATION_JSON));

        UserLimitResponseDTO result = accountClient.getUserLimits(10);
        assertThat(result).isNotNull();
        assertThat(result.getMaxAmountPerTransactionRon()).isEqualByComparingTo("5000.00");
        mockServer.verify();
    }

    @Test
    @DisplayName("getUserAccounts returns list on 200 OK")
    void testGetUserAccountsSuccess() throws Exception {
        List<AccountInternalSummaryDTO> expected = List.of(
                new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "RON acc", "RON", new BigDecimal("1000.00"), "ACTIVE")
        );

        mockServer.expect(MockRestRequestMatchers.requestTo("http://localhost:8082/api/internal/accounts/user/10"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withSuccess(objectMapper.writeValueAsString(expected), MediaType.APPLICATION_JSON));

        List<AccountInternalSummaryDTO> result = accountClient.getUserAccounts(10);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccountId()).isEqualTo(1L);
        mockServer.verify();
    }
}
