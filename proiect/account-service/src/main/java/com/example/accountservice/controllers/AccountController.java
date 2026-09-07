package com.example.accountservice.controllers;

import com.example.accountservice.dto.*;
import com.example.accountservice.services.AccountService;
import com.example.accountservice.services.CurrentUserService;
import com.example.accountservice.services.ResourceAuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final CurrentUserService currentUserService;
    private final ResourceAuthorizationService authorizationService;
    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponseDTO> createAccount(
            @Valid @RequestBody CreateSingleAccountRequestDTO dto,
            @RequestParam(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        AccountResponseDTO response = accountService.createSingleAccount(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AccountSummaryDTO>> getAccounts(
            @RequestParam(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        List<AccountSummaryDTO> response = accountService.getActiveAccountsForUser(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/paged")
    public ResponseEntity<PageResponseDTO<AccountSummaryDTO>> getAccountsPaged(
            @RequestParam(name = "userId", required = false) Integer requestedUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "alias") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        PageResponseDTO<AccountSummaryDTO> response = accountService.getActiveAccountsForUserPaged(userId, page, size, sortBy, direction);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary/currency")
    public ResponseEntity<List<AccountCurrencySummaryDTO>> getCurrencySummary(
            @RequestParam(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        List<AccountCurrencySummaryDTO> response = accountService.getAccountCurrencySummary(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId:\\d+}")
    public ResponseEntity<AccountDetailsDTO> getAccountDetails(
            @PathVariable Long accountId,
            @RequestParam(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        authorizationService.requireAccount(userId, accountId, ResourceAuthorizationService.AccountPermission.READ);
        AccountDetailsDTO response = accountService.getAccountDetails(accountId, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{accountId:\\d+}/close")
    public ResponseEntity<Void> closeAccount(
            @PathVariable Long accountId,
            @RequestParam(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        authorizationService.requireAccount(userId, accountId, ResourceAuthorizationService.AccountPermission.OWNER);
        accountService.closeAccount(accountId, userId);
        return ResponseEntity.ok().build();
    }
}
