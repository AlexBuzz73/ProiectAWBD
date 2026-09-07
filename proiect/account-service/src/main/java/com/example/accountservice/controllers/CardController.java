package com.example.accountservice.controllers;

import com.example.accountservice.dto.CardResponseDTO;
import com.example.accountservice.services.CardService;
import com.example.accountservice.services.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    private final CurrentUserService currentUserService;

    @PostMapping({
            "/api/accounts/{accountId}/cards",
            "/api/accounts/{accountId}/card",
            "/api/users/{userId}/accounts/{accountId}/card",
            "/api/users/me/accounts/{accountId}/card"
    })
    public ResponseEntity<CardResponseDTO> createCard(
            @PathVariable Long accountId,
            @PathVariable(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        String username = currentUserService.getCurrentUsername();
        CardResponseDTO response = cardService.createCard(userId, accountId, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping({
            "/api/accounts/{accountId}/cards",
            "/api/accounts/{accountId}/card",
            "/api/users/{userId}/accounts/{accountId}/card",
            "/api/users/me/accounts/{accountId}/card"
    })
    public ResponseEntity<?> getCards(
            @PathVariable Long accountId,
            @PathVariable(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        List<CardResponseDTO> cards = cardService.getCardsForAccount(userId, accountId);
        return ResponseEntity.ok(cards);
    }

    @PatchMapping({
            "/api/accounts/{accountId}/cards/{cardId}/status/{status}",
            "/api/accounts/{accountId}/card/{cardId}/status/{status}",
            "/api/users/{userId}/accounts/{accountId}/card/{cardId}/status/{status}",
            "/api/users/me/accounts/{accountId}/card/{cardId}/status/{status}"
    })
    public ResponseEntity<Void> updateCardStatus(
            @PathVariable Long accountId,
            @PathVariable Integer cardId,
            @PathVariable String status,
            @PathVariable(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        cardService.updateCardStatus(userId, accountId, cardId, status);
        return ResponseEntity.ok().build();
    }

    @PutMapping({
            "/api/accounts/{accountId}/cards/{cardId}/status/{status}",
            "/api/accounts/{accountId}/card/{cardId}/status/{status}"
    })
    public ResponseEntity<Void> updateCardStatusPut(
            @PathVariable Long accountId,
            @PathVariable Integer cardId,
            @PathVariable String status,
            @PathVariable(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        cardService.updateCardStatus(userId, accountId, cardId, status);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping({
            "/api/accounts/{accountId}/cards/{cardId}",
            "/api/accounts/{accountId}/cards/{cardId}/delete",
            "/api/accounts/{accountId}/card/{cardId}/delete",
            "/api/users/{userId}/accounts/{accountId}/card/{cardId}/delete",
            "/api/users/me/accounts/{accountId}/card/{cardId}/delete"
    })
    public ResponseEntity<Void> deleteCard(
            @PathVariable Long accountId,
            @PathVariable Integer cardId,
            @PathVariable(name = "userId", required = false) Integer requestedUserId) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        cardService.deleteCard(userId, accountId, cardId);
        return ResponseEntity.noContent().build();
    }
}
