package com.example.demo.controllers;

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

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponseDTO> createSingleAccount(@Valid @RequestBody CreateSingleAccountRequestDTO dto, @RequestParam int userId) {
        AccountResponseDTO response = accountService.createSingleAccount(dto, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<AccountSummaryDTO>> getActiveAccountsForUser(@RequestParam int userId) {
        List<AccountSummaryDTO> response = accountService.getActiveAccountsForUser(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/paged")
    public ResponseEntity<PageResponseDTO<AccountSummaryDTO>> getActiveAccountsForUserPaged(
            @RequestParam int userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2") int size,
            @RequestParam(defaultValue = "alias") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        PageResponseDTO<AccountSummaryDTO> response = accountService.getActiveAccountsForUserPaged(userId, page, size, sortBy, direction);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary/currency")
    public ResponseEntity<List<AccountCurrencySummaryDTO>> getAccountCurrencySummary(@RequestParam int userId) {
        List<AccountCurrencySummaryDTO> response = accountService.getAccountCurrencySummary(userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId:\\d+}")
    public ResponseEntity<AccountDetailsDTO> getAccountDetails(@PathVariable Long accountId, @RequestParam int userId) {
        AccountDetailsDTO response = accountService.getAccountDetails(accountId, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{accountId:\\d+}/close")
    public ResponseEntity<Void> closeAccount(@PathVariable Long accountId, @RequestParam int userId) {
        accountService.closeAccount(accountId, userId);
        return ResponseEntity.ok().build();
    }
}
