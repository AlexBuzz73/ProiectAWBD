package com.example.demo.controllers;

import com.example.demo.services.CurrentUserService;
import com.example.demo.services.ResourceAuthorizationService;
import static com.example.demo.services.ResourceAuthorizationService.AccountPermission.*;

import com.example.demo.dto.*;
import com.example.demo.services.CardService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/users/{userId}/accounts/{accountId}/card", "/api/users/me/accounts/{accountId}/card"})
public class CardController {
    private final CurrentUserService currentUserService;
    private final ResourceAuthorizationService authorization;
    private final CardService cardService;

    public CardController(CardService cardService, CurrentUserService currentUserService, ResourceAuthorizationService authorization) {
        this.cardService = cardService;
        this.currentUserService = currentUserService;
        this.authorization = authorization;
    }

    @PostMapping
    public CardResponseDTO createCard(
            @PathVariable(name = "userId", required = false) Integer requestedUserId,
            @PathVariable Long accountId
    ) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        authorization.requireAccount(userId, accountId, OWNER);
        return cardService.createCard(userId, accountId);
    }

    @GetMapping
    public CardResponseDTO getCardForAccount(
            @PathVariable(name = "userId", required = false) Integer requestedUserId,
            @PathVariable Long accountId
    ) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        authorization.requireAccount(userId, accountId, READ);
        return cardService.getCardFromAccount(userId, accountId);
    }

    @DeleteMapping("/{cardId}/delete")
    public void deleteCard(
            @PathVariable(name = "userId", required = false) Integer requestedUserId,
            @PathVariable Long accountId,
            @PathVariable Integer cardId
    ) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        authorization.requireAccount(userId, accountId, OWNER);
        authorization.requireCard(accountId, cardId);
        cardService.deleteCard(userId, accountId);
    }

    @PatchMapping("/{cardId}/status/{status}")
    public void updateCard(
            @PathVariable(name = "userId", required = false) Integer requestedUserId,
            @PathVariable Long accountId,
            @PathVariable Integer cardId,
            @PathVariable String status
    ) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        authorization.requireAccount(userId, accountId, OWNER);
        authorization.requireCard(accountId, cardId);
        cardService.updateCard(userId, accountId, cardId, status);
    }
}
