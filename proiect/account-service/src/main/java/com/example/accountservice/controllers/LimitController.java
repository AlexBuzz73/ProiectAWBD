package com.example.accountservice.controllers;

import com.example.accountservice.dto.BankLimitRequestDTO;
import com.example.accountservice.dto.BankLimitResponseDTO;
import com.example.accountservice.dto.UserLimitRequestDTO;
import com.example.accountservice.dto.UserLimitResponseDTO;
import com.example.accountservice.services.CurrentUserService;
import com.example.accountservice.services.LimitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class LimitController {

    private final LimitService limitService;
    private final CurrentUserService currentUserService;

    @GetMapping({"/api/limits/me", "/api/user/me/limits", "/api/user/{userId}/limits"})
    public ResponseEntity<UserLimitResponseDTO> getUserLimits(
            @PathVariable(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        UserLimitResponseDTO response = limitService.getUserLimits(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping({"/api/limits/me", "/api/user/me/limits", "/api/user/{userId}/limits"})
    public ResponseEntity<UserLimitResponseDTO> updateUserLimits(
            @Valid @RequestBody UserLimitRequestDTO dto,
            @PathVariable(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        UserLimitResponseDTO response = limitService.updateUserLimits(userId, dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping({"/api/limits/me", "/api/user/me/limits", "/api/user/{userId}/limits"})
    public ResponseEntity<Void> deleteUserLimits(
            @PathVariable(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        limitService.deleteUserLimits(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/admin/bank-limits")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BankLimitResponseDTO> getBankLimits() {
        BankLimitResponseDTO response = limitService.getBankLimits();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/api/admin/bank-limits")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BankLimitResponseDTO> updateBankLimits(@Valid @RequestBody BankLimitRequestDTO dto) {
        BankLimitResponseDTO response = limitService.updateBankLimits(dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/admin/bank-limits/{bankLimitId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBankLimits(@PathVariable Integer bankLimitId) {
        limitService.deleteBankLimits(bankLimitId);
        return ResponseEntity.noContent().build();
    }
}
