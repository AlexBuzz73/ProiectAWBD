package com.example.demo.controllers;

import com.example.demo.services.CurrentUserService;
import com.example.demo.services.ResourceAuthorizationService;
import static com.example.demo.services.ResourceAuthorizationService.AccountPermission.*;

import com.example.demo.domain.Transaction;
import com.example.demo.domain.User;
import com.example.demo.dto.CurrencyExchangeDTO;
import com.example.demo.dto.OwnAccountTransferDTO;
import com.example.demo.dto.PaymentRequestDTO;
import com.example.demo.dto.TransactionSummaryDTO;
import com.example.demo.mappers.TransactionMapper;
import com.example.demo.services.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

        User user = currentUserService.getCurrentUser();

        authorization.requireAccount(userId, dto.getSourceAccountId().longValue(), PAY);
        authorization.requireCategory(userId, dto.getCategoryId(), false);
        Transaction transaction = transactionService.initiatePayment(dto, user);

        return ResponseEntity.ok(transactionMapper.toTransactionSummaryDTO(transaction, userId));
    }

    @PostMapping("/transfer-own")
    public ResponseEntity<TransactionSummaryDTO> transferOwnAccounts(@Valid @RequestBody OwnAccountTransferDTO dto, @RequestParam(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        User user = currentUserService.getCurrentUser();

        authorization.requireAccount(userId, dto.getSourceAccountId().longValue(), PAY);
        authorization.requireAccount(userId, dto.getDestinationAccountId().longValue(), READ);
        authorization.requireCategory(userId, dto.getCategoryId(), false);
        Transaction transaction = transactionService.transferBetweenOwnAccounts(dto, user);

        return ResponseEntity.ok(transactionMapper.toTransactionSummaryDTO(transaction, userId));
    }

    @PostMapping("/exchange")
    public ResponseEntity<TransactionSummaryDTO> exchangeCurrency(
            @Valid @RequestBody CurrencyExchangeDTO dto,
            @RequestParam(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);

        User user = currentUserService.getCurrentUser();

        authorization.requireAccount(userId, dto.getSourceAccountId().longValue(), PAY);
        authorization.requireAccount(userId, dto.getDestinationAccountId().longValue(), READ);
        authorization.requireCategory(userId, dto.getCategoryId(), false);
        Transaction transaction = transactionService.performCurrencyExchange(dto, user);

        return ResponseEntity.ok(transactionMapper.toTransactionSummaryDTO(transaction, userId));
    }
}
