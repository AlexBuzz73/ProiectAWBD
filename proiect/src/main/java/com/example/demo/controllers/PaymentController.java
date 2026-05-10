package com.example.demo.controllers;

import com.example.demo.domain.Transaction;
import com.example.demo.domain.User;
import com.example.demo.dto.PaymentRequestDTO;
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

    @PostMapping("/initiate")
    public ResponseEntity<Transaction> initiatePayment(
            @Valid @RequestBody PaymentRequestDTO dto,
            @RequestParam int userId) {


        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost gasit."));


        Transaction transaction = transactionService.initiatePayment(dto, user);

        return ResponseEntity.ok(transaction);
    }
}