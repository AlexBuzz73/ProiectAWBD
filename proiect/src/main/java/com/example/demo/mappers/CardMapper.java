package com.example.demo.mappers;

import com.example.demo.domain.Card;
import com.example.demo.dto.CardResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class CardMapper {

    public CardResponseDTO toCardResponseDTO(Card card) {
        CardResponseDTO cardResponseDTO = new CardResponseDTO();
        cardResponseDTO.setCardNumber(card.getCardNumber());
        cardResponseDTO.setType(card.getType());
        cardResponseDTO.setExpirationDate(card.getExpirationDate());
        cardResponseDTO.setHolderName(card.getHolderName());
        cardResponseDTO.setStatus(card.getStatus());
        return cardResponseDTO;
    }
}
