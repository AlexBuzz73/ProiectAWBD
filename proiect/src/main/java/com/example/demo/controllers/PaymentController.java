package com.example.demo.controllers;

import com.example.demo.domain.Transaction;
import com.example.demo.domain.User;
import com.example.demo.dto.CurrencyExchangeDTO;
import com.example.demo.dto.OwnAccountTransferDTO;
import com.example.demo.dto.PaymentRequestDTO;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;

    @PostMapping("/initiate")
    public ResponseEntity<Transaction> initiatePayment(
            @Valid @RequestBody PaymentRequestDTO dto,
            @RequestParam int userId) {


        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost gasit."));


        Transaction transaction = transactionService.initiatePayment(dto, user);

        return ResponseEntity.ok(transaction);
    }
    @PostMapping("/transfer-own")
    public ResponseEntity<?> transferOwnAccounts(@RequestBody OwnAccountTransferDTO dto, @RequestParam int userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(transactionService.transferBetweenOwnAccounts(dto, user));
    }
    @PostMapping("/exchange")
    public ResponseEntity<?> exchangeCurrency(
            @RequestBody CurrencyExchangeDTO dto,
            @RequestParam int userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(transactionService.performCurrencyExchange(dto, user));
    }
}