package com.example.demo.controllers;

import com.example.demo.services.CurrentUserService;
import com.example.demo.services.ResourceAuthorizationService;
import static com.example.demo.services.ResourceAuthorizationService.AccountPermission.*;

import com.example.demo.dto.*;
import com.example.demo.services.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final CurrentUserService currentUserService;
    private final ResourceAuthorizationService authorization;

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponseDTO> createSingleAccount(@Valid @RequestBody CreateSingleAccountRequestDTO dto, @RequestParam(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        AccountResponseDTO response = accountService.createSingleAccount(dto, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<AccountSummaryDTO>> getActiveAccountsForUser(@RequestParam(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        List<AccountSummaryDTO> response = accountService.getActiveAccountsForUser(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/paged")
    public ResponseEntity<PageResponseDTO<AccountSummaryDTO>> getActiveAccountsForUserPaged(
            @RequestParam(name = "userId", required = false) Integer requestedUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2") int size,
            @RequestParam(defaultValue = "alias") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        PageResponseDTO<AccountSummaryDTO> response = accountService.getActiveAccountsForUserPaged(userId, page, size, sortBy, direction);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary/currency")
    public ResponseEntity<List<AccountCurrencySummaryDTO>> getAccountCurrencySummary(@RequestParam(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        List<AccountCurrencySummaryDTO> response = accountService.getAccountCurrencySummary(userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId:\\d+}")
    public ResponseEntity<AccountDetailsDTO> getAccountDetails(@PathVariable Long accountId, @RequestParam(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        authorization.requireAccount(userId, accountId, READ);
        AccountDetailsDTO response = accountService.getAccountDetails(accountId, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{accountId:\\d+}/close")
    public ResponseEntity<Void> closeAccount(@PathVariable Long accountId, @RequestParam(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        authorization.requireAccount(userId, accountId, OWNER);
        accountService.closeAccount(accountId, userId);
        return ResponseEntity.ok().build();
    }
}
