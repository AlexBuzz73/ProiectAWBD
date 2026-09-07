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
}
