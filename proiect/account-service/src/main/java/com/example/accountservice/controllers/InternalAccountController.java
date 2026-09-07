package com.example.accountservice.controllers;

import com.example.accountservice.dto.*;
import com.example.accountservice.services.AccountService;
import com.example.accountservice.services.LimitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal/accounts")
@RequiredArgsConstructor
public class InternalAccountController {

    private final AccountService accountService;
    private final LimitService limitService;
    private final com.example.accountservice.client.UserClient userClient;

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountInternalSummaryDTO> getAccountSummary(@PathVariable Long accountId) {
        AccountInternalSummaryDTO summary = accountService.getAccountInternalSummary(accountId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/by-iban/{iban}")
    public ResponseEntity<AccountInternalSummaryDTO> getAccountSummaryByIban(@PathVariable String iban) {
        AccountInternalSummaryDTO summary = accountService.getAccountInternalSummaryByIban(iban);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/{accountId}/debit")
    public ResponseEntity<AccountInternalSummaryDTO> debitAccount(
            @PathVariable Long accountId,
            @Valid @RequestBody DebitRequestDTO dto) {
        AccountInternalSummaryDTO summary = accountService.debitAccount(accountId, dto.getAmount(), dto.getOperationId());
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/{accountId}/credit")
    public ResponseEntity<AccountInternalSummaryDTO> creditAccount(
            @PathVariable Long accountId,
            @Valid @RequestBody CreditRequestDTO dto) {
        AccountInternalSummaryDTO summary = accountService.creditAccount(accountId, dto.getAmount(), dto.getOperationId());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{accountId}/access-check")
    public ResponseEntity<Boolean> checkUserAccountAccess(
            @PathVariable Long accountId,
            @RequestParam Integer userId,
            @RequestParam(required = false) String requiredRole) {
        boolean hasAccess = accountService.checkUserAccountAccess(accountId, userId, requiredRole);
        return ResponseEntity.ok(hasAccess);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccountInternalSummaryDTO>> getUserAccounts(@PathVariable Integer userId) {
        List<AccountInternalSummaryDTO> summaries = accountService.getActiveAccountInternalSummariesForUser(userId);
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/limits/user/{userId}")
    public ResponseEntity<UserLimitResponseDTO> getUserLimits(@PathVariable Integer userId) {
        UserLimitResponseDTO limits = limitService.getUserLimits(userId);
        return ResponseEntity.ok(limits);
    }

    @GetMapping("/feign-test/user/{userId}")
    public ResponseEntity<java.util.Map<String, Object>> testFeignToUserService(@PathVariable Integer userId) {
        var user = userClient.findUserById(userId);
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("user", user.orElse(null));
        map.put("callerService", "account-service");
        return ResponseEntity.ok(map);
    }

    @org.springframework.beans.factory.annotation.Value("${server.port:8082}")
    private int serverPort;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.example.accountservice.client.UserFeignClient userFeignClient;

    @GetMapping("/instance-info")
    public ResponseEntity<java.util.Map<String, Object>> getInstanceInfo() {
        return ResponseEntity.ok(java.util.Map.of("service", "account-service", "port", serverPort));
    }

    @GetMapping("/feign-test/lb")
    public ResponseEntity<java.util.Map<String, Object>> testFeignLb() {
        if (userFeignClient != null) {
            return ResponseEntity.ok(userFeignClient.getInstanceInfo());
        }
        return ResponseEntity.ok(java.util.Map.of("status", "mock", "service", "account-service"));
    }
}
