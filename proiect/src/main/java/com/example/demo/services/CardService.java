package com.example.demo.services;

import com.example.demo.dto.CardResponseDTO;

public interface CardService {

    CardResponseDTO createCard(Integer userId, Long accountId);
    void deleteCard(Integer userId, Long accountId);
    void updateCard(Integer userId, Long accountId, Integer cardId, String status);
    CardResponseDTO getCardFromAccount(Integer userId, Long accountId);
}
