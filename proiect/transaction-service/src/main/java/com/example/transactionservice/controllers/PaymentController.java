package com.example.transactionservice.controllers;

import com.example.transactionservice.domain.Transaction;
import com.example.transactionservice.dto.CurrencyExchangeDTO;
import com.example.transactionservice.dto.OwnAccountTransferDTO;
import com.example.transactionservice.dto.PaymentRequestDTO;
import com.example.transactionservice.dto.TransactionSummaryDTO;
import com.example.transactionservice.mappers.TransactionMapper;
import com.example.transactionservice.services.CurrentUserService;
import com.example.transactionservice.services.ResourceAuthorizationService;
import com.example.transactionservice.services.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.example.transactionservice.services.ResourceAuthorizationService.AccountPermission.PAY;
import static com.example.transactionservice.services.ResourceAuthorizationService.AccountPermission.READ;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final CurrentUserService currentUserService;
    private final ResourceAuthorizationService authorization;
    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    @PostMapping("/initiate")
    public ResponseEntity<TransactionSummaryDTO> initiatePayment(
            @Valid @RequestBody PaymentRequestDTO dto,
            @RequestParam(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);

        authorization.requireAccount(userId, dto.getSourceAccountId(), PAY);
        authorization.requireCategory(userId, dto.getCategoryId(), false);
        Transaction transaction = transactionService.initiatePayment(dto, userId);

        return ResponseEntity.ok(transactionMapper.toTransactionSummaryDTO(transaction, userId));
    }

    @PostMapping("/transfer-own")
    public ResponseEntity<TransactionSummaryDTO> transferOwnAccounts(
            @Valid @RequestBody OwnAccountTransferDTO dto,
            @RequestParam(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);

        authorization.requireAccount(userId, dto.getSourceAccountId(), PAY);
        authorization.requireAccount(userId, dto.getDestinationAccountId(), READ);
        authorization.requireCategory(userId, dto.getCategoryId(), false);
        Transaction transaction = transactionService.transferBetweenOwnAccounts(dto, userId);

        return ResponseEntity.ok(transactionMapper.toTransactionSummaryDTO(transaction, userId));
    }

    @PostMapping("/exchange")
    public ResponseEntity<TransactionSummaryDTO> exchangeCurrency(
            @Valid @RequestBody CurrencyExchangeDTO dto,
            @RequestParam(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);

        authorization.requireAccount(userId, dto.getSourceAccountId(), PAY);
        authorization.requireAccount(userId, dto.getDestinationAccountId(), READ);
        authorization.requireCategory(userId, dto.getCategoryId(), false);
        Transaction transaction = transactionService.performCurrencyExchange(dto, userId);

        return ResponseEntity.ok(transactionMapper.toTransactionSummaryDTO(transaction, userId));
    }
}
