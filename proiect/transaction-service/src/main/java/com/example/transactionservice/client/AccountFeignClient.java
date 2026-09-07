package com.example.transactionservice.client;

import com.example.transactionservice.dto.AccountInternalSummaryDTO;
import com.example.transactionservice.dto.CreditRequestDTO;
import com.example.transactionservice.dto.DebitRequestDTO;
import com.example.transactionservice.dto.UserLimitResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "account-service", configuration = FeignClientConfig.class)
public interface AccountFeignClient {

    @GetMapping("/api/internal/accounts/{accountId}")
    AccountInternalSummaryDTO getAccount(@PathVariable("accountId") Long accountId);

    @GetMapping("/api/internal/accounts/by-iban/{iban}")
    AccountInternalSummaryDTO getAccountByIban(@PathVariable("iban") String iban);

    @PostMapping("/api/internal/accounts/{accountId}/debit")
    AccountInternalSummaryDTO debit(@PathVariable("accountId") Long accountId, @RequestBody DebitRequestDTO dto);

    @PostMapping("/api/internal/accounts/{accountId}/credit")
    AccountInternalSummaryDTO credit(@PathVariable("accountId") Long accountId, @RequestBody CreditRequestDTO dto);

    @GetMapping("/api/internal/accounts/{accountId}/access-check")
    Boolean checkAccess(
            @PathVariable("accountId") Long accountId,
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "requiredRole", required = false) String requiredRole);

    @GetMapping("/api/internal/accounts/limits/user/{userId}")
    UserLimitResponseDTO getUserLimits(@PathVariable("userId") Integer userId);

    @GetMapping("/api/internal/accounts/user/{userId}")
    List<AccountInternalSummaryDTO> getUserAccounts(@PathVariable("userId") Integer userId);
}
