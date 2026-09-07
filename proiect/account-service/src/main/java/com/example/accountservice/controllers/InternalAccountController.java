package com.example.accountservice.controllers;

import com.example.accountservice.dto.AccountInternalSummaryDTO;
import com.example.accountservice.dto.CreditRequestDTO;
import com.example.accountservice.dto.DebitRequestDTO;
import com.example.accountservice.services.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/accounts")
@RequiredArgsConstructor
public class InternalAccountController {

    private final AccountService accountService;

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
}
