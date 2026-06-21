package com.example.demo.controllers;

import com.example.demo.domain.Account;
import com.example.demo.dto.AccountResponseDTO;
import com.example.demo.dto.BankLimitUpdateDTO;
import com.example.demo.dto.SharedAccountRequest;
import com.example.demo.mappers.AccountMapper;
import com.example.demo.services.AdminService;
import com.example.demo.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AuthService authService;
    private final AccountMapper accountMapper;

    @PutMapping("/users/{userId}/unlock")
    public ResponseEntity<Void> unlockUser(@PathVariable int userId) {
        authService.unlockUser(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unlock-user")
    public ResponseEntity<String> unlockUserByEmail(@RequestParam String email) {
        authService.unlockUserByEmail(email);
        return ResponseEntity.ok("User unlocked successfully.");
    }

    @PostMapping("/create-shared-account")
    public ResponseEntity<AccountResponseDTO> createSharedAccount(@RequestBody SharedAccountRequest dto) {
        Account account = adminService.createSharedAccount(dto);
        return ResponseEntity.ok(accountMapper.toAccountResponseDTO(account));
    }

    @DeleteMapping("/accounts/{accountId}/access")
    public ResponseEntity<Void> revokeAccountAccess(@PathVariable Long accountId, @RequestParam String email) {
        adminService.revokeAccountAccess(accountId, email);
        return ResponseEntity.ok().build();
    }
}
