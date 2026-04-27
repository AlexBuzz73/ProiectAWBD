package com.example.demo.controllers;

import com.example.demo.dto.AccountDetailsDTO;
import com.example.demo.dto.AccountResponseDTO;
import com.example.demo.dto.AccountSummaryDTO;
import com.example.demo.dto.CreateSingleAccountRequestDTO;
import com.example.demo.services.AccountService;
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
    public ResponseEntity<AccountResponseDTO> createSingleAccount(@RequestBody CreateSingleAccountRequestDTO dto, @RequestParam int userId) {
        AccountResponseDTO response = accountService.createSingleAccount(dto, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<AccountSummaryDTO>> getActiveAccountsForUser(@RequestParam int userId) {
        List<AccountSummaryDTO> response = accountService.getActiveAccountsForUser(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountDetailsDTO> getAccountDetails(@PathVariable Long accountId, @RequestParam int userId) {
        AccountDetailsDTO response = accountService.getAccountDetails(accountId, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{accountId}/close")
    public ResponseEntity<Void> closeAccount(@PathVariable Long accountId, @RequestParam int userId) {
        accountService.closeAccount(accountId, userId);
        return ResponseEntity.ok().build();
    }
}
