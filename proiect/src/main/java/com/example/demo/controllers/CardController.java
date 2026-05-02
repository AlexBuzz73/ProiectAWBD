package com.example.demo.controllers;

import com.example.demo.dto.*;
import com.example.demo.services.CardService;
import com.example.demo.services.LimitService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/accounts/{accountId}/card")
public class CardController {
    private final CardService cardService;


    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    public CardResponseDTO createCard(
            @PathVariable Integer userId,
            @PathVariable Long accountId
    ) {
        return cardService.createCard(userId, accountId);
    }

    @GetMapping
    public CardResponseDTO getActiveCardForAccount(
            @PathVariable Integer userId,
            @PathVariable Long accountId
    ) {
        return cardService.getActiveCardFromAccount(userId, accountId);
    }

//    @GetMapping("/test")
//    public String test() {
//        return "merge";
//    }

    @PatchMapping("/{cardId}/delete")
    public void deleteCard(
            @PathVariable Integer userId,
            @PathVariable Long accountId,
            @PathVariable Integer cardId
    ) {
        cardService.deleteCard(userId, accountId);
    }

//    @PatchMapping("/{cardId}/block")
//    public void blockCard(
//            @PathVariable Integer userId,
//            @PathVariable Long accountId,
//            @PathVariable Integer cardId
//    ) {
//        cardService.blockCard(userId, accountId, cardId);
//    }
//
//    @PatchMapping("/{cardId}/unblock")
//    public void unblockCard(
//            @PathVariable Integer userId,
//            @PathVariable Long accountId,
//            @PathVariable Integer cardId
//    ) {
//        cardService.unblockCard(userId, accountId, cardId);
//    }

    @PatchMapping("/{cardId}/status")
    public void updateCard(
            @PathVariable Integer userId,
            @PathVariable Long accountId,
            @PathVariable Integer cardId,
            @PathVariable String status
    ) {
        cardService.updateCard(userId, accountId, cardId, status);
    }
}
