package com.example.accountservice.controllers;

import com.example.accountservice.dto.AccountResponseDTO;
import com.example.accountservice.dto.SharedAccountRequest;
import com.example.accountservice.services.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccountController {

    private final AccountService accountService;

    @PostMapping({"/accounts/shared", "/create-shared-account"})
    public ResponseEntity<AccountResponseDTO> createSharedAccount(@Valid @RequestBody SharedAccountRequest dto) {
        AccountResponseDTO response = accountService.createSharedAccount(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/accounts/{accountId}/access")
    public ResponseEntity<Void> revokeAccountAccess(@PathVariable Long accountId, @RequestParam String email) {
        accountService.revokeAccountAccess(accountId, email);
        return ResponseEntity.noContent().build();
    }
}
