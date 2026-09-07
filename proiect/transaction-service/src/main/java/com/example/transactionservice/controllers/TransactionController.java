package com.example.transactionservice.controllers;

import com.example.transactionservice.dto.PageResponseDTO;
import com.example.transactionservice.dto.TransactionSummaryDTO;
import com.example.transactionservice.services.CurrentUserService;
import com.example.transactionservice.services.ResourceAuthorizationService;
import com.example.transactionservice.services.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.example.transactionservice.services.ResourceAuthorizationService.AccountPermission.READ;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final CurrentUserService currentUserService;
    private final ResourceAuthorizationService authorization;
    private final TransactionService transactionService;

    @GetMapping("/user")
    public ResponseEntity<PageResponseDTO<TransactionSummaryDTO>> getTransactionsForUserPaged(
            @RequestParam(name = "userId", required = false) Integer requestedUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        PageResponseDTO<TransactionSummaryDTO> response = transactionService.getTransactionsForUserPaged(userId, page, size, sortBy, direction);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountId:\\d+}")
    public ResponseEntity<PageResponseDTO<TransactionSummaryDTO>> getTransactionsForAccountPaged(
            @PathVariable Long accountId,
            @RequestParam(name = "userId", required = false) Integer requestedUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        authorization.requireAccount(userId, accountId, READ);
        PageResponseDTO<TransactionSummaryDTO> response = transactionService.getTransactionsForAccountPaged(accountId, userId, page, size, sortBy, direction);
        return ResponseEntity.ok(response);
    }
}
