package com.example.accountservice;

import com.example.accountservice.client.UserClient;
import com.example.accountservice.client.UserFeignClient;
import com.example.accountservice.config.InstanceIdFilter;
import com.example.accountservice.controllers.InternalAccountController;
import com.example.accountservice.dto.*;
import com.example.accountservice.services.AccountService;
import com.example.accountservice.services.LimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.info.Info;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalAccountControllerTest {

    @Mock
    private AccountService accountService;

    @Mock
    private LimitService limitService;

    @Mock
    private UserClient userClient;

    @Mock
    private UserFeignClient userFeignClient;

    @InjectMocks
    private InternalAccountController controller;

    @Test
    @DisplayName("getAccountSummary returns account summary DTO")
    void testGetAccountSummary() {
        AccountInternalSummaryDTO dto = new AccountInternalSummaryDTO();
        dto.setAccountId(1L);
        dto.setIban("RO11BANK0000000000000001");
        dto.setBalance(BigDecimal.valueOf(1000));
        when(accountService.getAccountInternalSummary(1L)).thenReturn(dto);

        ResponseEntity<AccountInternalSummaryDTO> response = controller.getAccountSummary(1L);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccountId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getAccountSummaryByIban returns summary")
    void testGetAccountSummaryByIban() {
        AccountInternalSummaryDTO dto = new AccountInternalSummaryDTO();
        dto.setAccountId(2L);
        dto.setIban("RO11BANK0000000000000002");
        when(accountService.getAccountInternalSummaryByIban("RO11BANK0000000000000002")).thenReturn(dto);

        ResponseEntity<AccountInternalSummaryDTO> response = controller.getAccountSummaryByIban("RO11BANK0000000000000002");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getIban()).isEqualTo("RO11BANK0000000000000002");
    }

    @Test
    @DisplayName("debitAccount calls service debit")
    void testDebitAccount() {
        DebitRequestDTO req = new DebitRequestDTO();
        req.setAmount(BigDecimal.valueOf(100));
        req.setOperationId("op-1");

        AccountInternalSummaryDTO summary = new AccountInternalSummaryDTO();
        summary.setBalance(BigDecimal.valueOf(900));
        when(accountService.debitAccount(1L, req.getAmount(), "op-1")).thenReturn(summary);

        ResponseEntity<AccountInternalSummaryDTO> res = controller.debitAccount(1L, req);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody().getBalance()).isEqualTo(BigDecimal.valueOf(900));
    }

    @Test
    @DisplayName("creditAccount calls service credit")
    void testCreditAccount() {
        CreditRequestDTO req = new CreditRequestDTO();
        req.setAmount(BigDecimal.valueOf(100));
        req.setOperationId("op-2");

        AccountInternalSummaryDTO summary = new AccountInternalSummaryDTO();
        summary.setBalance(BigDecimal.valueOf(1100));
        when(accountService.creditAccount(1L, req.getAmount(), "op-2")).thenReturn(summary);

        ResponseEntity<AccountInternalSummaryDTO> res = controller.creditAccount(1L, req);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody().getBalance()).isEqualTo(BigDecimal.valueOf(1100));
    }

    @Test
    @DisplayName("checkUserAccountAccess calls service")
    void testCheckUserAccountAccess() {
        when(accountService.checkUserAccountAccess(1L, 5, "OWNER")).thenReturn(true);
        ResponseEntity<Boolean> res = controller.checkUserAccountAccess(1L, 5, "OWNER");
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).isTrue();
    }

    @Test
    @DisplayName("getUserAccounts returns user account summaries")
    void testGetUserAccounts() {
        AccountInternalSummaryDTO acc = new AccountInternalSummaryDTO();
        acc.setAccountId(1L);
        when(accountService.getActiveAccountInternalSummariesForUser(5)).thenReturn(List.of(acc));

        ResponseEntity<List<AccountInternalSummaryDTO>> res = controller.getUserAccounts(5);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("getUserLimits returns limits")
    void testGetUserLimits() {
        UserLimitResponseDTO dto = new UserLimitResponseDTO();
        dto.setUserLimitId(5);
        when(limitService.getUserLimits(5)).thenReturn(dto);
        ResponseEntity<UserLimitResponseDTO> res = controller.getUserLimits(5);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody().getUserLimitId()).isEqualTo(5);
    }

    @Test
    @DisplayName("testFeignToUserService calls userClient")
    void testTestFeignToUserService() {
        when(userClient.findUserById(5)).thenReturn(Optional.of(new UserLookupDTO()));
        ResponseEntity<Map<String, Object>> res = controller.testFeignToUserService(5);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody().get("callerService")).isEqualTo("account-service");
    }

    @Test
    @DisplayName("getInstanceInfo returns service and port")
    void testGetInstanceInfo() {
        ReflectionTestUtils.setField(controller, "serverPort", 8082);
        ResponseEntity<Map<String, Object>> res = controller.getInstanceInfo();
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody().get("service")).isEqualTo("account-service");
        assertThat(res.getBody().get("port")).isEqualTo(8082);
    }

    @Test
    @DisplayName("testFeignLb calls userFeignClient when available")
    void testTestFeignLb() {
        ReflectionTestUtils.setField(controller, "userFeignClient", userFeignClient);
        when(userFeignClient.getInstanceInfo()).thenReturn(Map.of("service", "user-service", "port", 8081));
        ResponseEntity<Map<String, Object>> res = controller.testFeignLb();
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody().get("service")).isEqualTo("user-service");
    }

    @Test
    @DisplayName("testFeignLb fallback when userFeignClient is null")
    void testTestFeignLbFallback() {
        ReflectionTestUtils.setField(controller, "userFeignClient", null);
        ResponseEntity<Map<String, Object>> res = controller.testFeignLb();
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody().get("status")).isEqualTo("mock");
    }

    @Test
    @DisplayName("InstanceIdFilter populates headers and actuator info")
    void testInstanceIdFilter() throws Exception {
        InstanceIdFilter filter = new InstanceIdFilter();
        ReflectionTestUtils.setField(filter, "serviceName", "account-service");
        ReflectionTestUtils.setField(filter, "port", 8082);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        ReflectionTestUtils.invokeMethod(filter, "doFilterInternal", req, resp, chain);
        verify(resp).setHeader("X-Instance-Id", "account-service:8082");
        verify(resp).setHeader("X-Service-Port", "8082");
        verify(chain).doFilter(req, resp);

        Info.Builder builder = new Info.Builder();
        filter.contribute(builder);
        Info info = builder.build();
        assertThat(info.getDetails().get("instanceId")).isEqualTo("account-service:8082");
    }
}
