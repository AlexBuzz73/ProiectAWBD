package com.example.demo.controllers;

import com.example.demo.dto.PageResponseDTO;
import com.example.demo.dto.TransactionSummaryDTO;
import com.example.demo.services.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/user")
    public ResponseEntity<PageResponseDTO<TransactionSummaryDTO>> getTransactionsForUserPaged(
            @RequestParam int userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        PageResponseDTO<TransactionSummaryDTO> response = transactionService.getTransactionsForUserPaged(userId, page, size, sortBy, direction);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountId:\\d+}")
    public ResponseEntity<PageResponseDTO<TransactionSummaryDTO>> getTransactionsForAccountPaged(
            @PathVariable Long accountId,
            @RequestParam int userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        PageResponseDTO<TransactionSummaryDTO> response = transactionService.getTransactionsForAccountPaged(accountId, userId, page, size, sortBy, direction);
        return ResponseEntity.ok(response);
    }
}
