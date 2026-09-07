package com.example.accountservice.services;

import com.example.accountservice.dto.CardResponseDTO;

import java.util.List;

public interface CardService {

    CardResponseDTO createCard(Integer userId, Long accountId, String holderName);

    CardResponseDTO getCardFromAccount(Integer userId, Long accountId);

    List<CardResponseDTO> getCardsForAccount(Integer userId, Long accountId);

    void updateCardStatus(Integer userId, Long accountId, Integer cardId, String status);

    void deleteCard(Integer userId, Long accountId, Integer cardId);
}
