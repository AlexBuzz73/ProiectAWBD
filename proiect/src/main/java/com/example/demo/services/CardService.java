package com.example.demo.services;

import com.example.demo.dto.CardResponseDTO;

public interface CardService {

    CardResponseDTO createCard(Integer userId, Long accountId);
    void blockCard(Integer userId, Long accountId, Integer cardId);
    void unblockCard(Integer userId, Long accountId, Integer cardId);
    void deleteCard(Integer userId, Long accountId);
    CardResponseDTO getActiveCardFromAccount(Integer userId, Long accountId);
}
