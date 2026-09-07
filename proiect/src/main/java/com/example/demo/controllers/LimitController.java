package com.example.demo.controllers;

import com.example.demo.services.CurrentUserService;

import com.example.demo.dto.BankLimitRequestDTO;
import com.example.demo.dto.BankLimitResponseDTO;
import com.example.demo.dto.UserLimitRequestDTO;
import com.example.demo.dto.UserLimitResponseDTO;
import com.example.demo.services.LimitService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api")
public class LimitController {
    private final CurrentUserService currentUserService;
    private final LimitService limitService;


    public LimitController(LimitService limitService, CurrentUserService currentUserService) {
        this.limitService = limitService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/admin/bank-limits")
    public BankLimitResponseDTO getBankLimits() {
        return limitService.getBankLimits();
    }


    @PutMapping("/admin/bank-limits")
    public BankLimitResponseDTO updateBankLimits(@Valid @RequestBody BankLimitRequestDTO bankLimitRequestDTO) {
        return limitService.updateBankLimits(bankLimitRequestDTO);
    }

    @GetMapping({"/user/{userId}/limits", "/user/me/limits"})
    public UserLimitResponseDTO getUserLimits(@PathVariable(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        return limitService.getUserLimits(userId);
    }

    @PutMapping({"/user/{userId}/limits", "/user/me/limits"})
    public UserLimitResponseDTO updateUserLimits(@Valid @RequestBody UserLimitRequestDTO userLimitRequestDTO, @PathVariable(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        return limitService.updateUserLimits(userId, userLimitRequestDTO);
    }
    
    @DeleteMapping({"/user/{userId}/limits", "/user/me/limits"})
    public void deleteUserLimits(@PathVariable(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        limitService.deleteUserLimits(userId);
    }

    @DeleteMapping("/admin/bank-limits/{bankLimitId}")
    public void deleteBankLimits(@PathVariable Integer bankLimitId) {
        limitService.deleteBankLimits(bankLimitId);
    }
    
    
}
