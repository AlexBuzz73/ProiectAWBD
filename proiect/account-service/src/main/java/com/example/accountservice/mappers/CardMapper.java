package com.example.accountservice.mappers;

import com.example.accountservice.domain.Card;
import com.example.accountservice.dto.CardResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class CardMapper {

    public CardResponseDTO toCardResponseDTO(Card card) {
        if (card == null) {
            return null;
        }
        CardResponseDTO dto = new CardResponseDTO();
        dto.setCardId(card.getCardId());
        dto.setCardNumber(card.getCardNumber());
        dto.setType(card.getType());
        dto.setExpirationDate(card.getExpirationDate());
        dto.setHolderName(card.getHolderName());
        dto.setStatus(card.getStatus());
        return dto;
    }
}
