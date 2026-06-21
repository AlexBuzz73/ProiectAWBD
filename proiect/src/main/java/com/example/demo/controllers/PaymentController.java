package com.example.demo.controllers;

import com.example.demo.domain.Transaction;
import com.example.demo.domain.User;
import com.example.demo.dto.CurrencyExchangeDTO;
import com.example.demo.dto.OwnAccountTransferDTO;
import com.example.demo.dto.PaymentRequestDTO;
import com.example.demo.dto.TransactionSummaryDTO;
import com.example.demo.mappers.TransactionMapper;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;
    private final TransactionMapper transactionMapper;

    @PostMapping("/initiate")
    public ResponseEntity<TransactionSummaryDTO> initiatePayment(
            @Valid @RequestBody PaymentRequestDTO dto,
            @RequestParam int userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit."));

        Transaction transaction = transactionService.initiatePayment(dto, user);

        return ResponseEntity.ok(transactionMapper.toTransactionSummaryDTO(transaction));
    }

    @PostMapping("/transfer-own")
    public ResponseEntity<TransactionSummaryDTO> transferOwnAccounts(@Valid @RequestBody OwnAccountTransferDTO dto, @RequestParam int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit."));

        Transaction transaction = transactionService.transferBetweenOwnAccounts(dto, user);

        return ResponseEntity.ok(transactionMapper.toTransactionSummaryDTO(transaction));
    }

    @PostMapping("/exchange")
    public ResponseEntity<TransactionSummaryDTO> exchangeCurrency(
            @Valid @RequestBody CurrencyExchangeDTO dto,
            @RequestParam int userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit."));

        Transaction transaction = transactionService.performCurrencyExchange(dto, user);

        return ResponseEntity.ok(transactionMapper.toTransactionSummaryDTO(transaction));
    }
}
