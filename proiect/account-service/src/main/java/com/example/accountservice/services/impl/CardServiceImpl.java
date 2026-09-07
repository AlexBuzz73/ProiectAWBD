package com.example.accountservice.services.impl;

import com.example.accountservice.domain.Account;
import com.example.accountservice.domain.Card;
import com.example.accountservice.dto.CardResponseDTO;
import com.example.accountservice.exceptions.ResourceNotFoundException;
import com.example.accountservice.mappers.CardMapper;
import com.example.accountservice.repositories.AccountRepository;
import com.example.accountservice.repositories.CardRepository;
import com.example.accountservice.services.CardService;
import com.example.accountservice.services.ResourceAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final CardMapper cardMapper;
    private final ResourceAuthorizationService authorizationService;

    @Override
    @Transactional
    public CardResponseDTO createCard(Integer userId, Long accountId, String holderName) {
        authorizationService.requireAccount(userId, accountId, ResourceAuthorizationService.AccountPermission.OPERATIONAL);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Contul cu id " + accountId + " nu a fost gasit"));

        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new IllegalArgumentException("Nu se pot emite carduri pentru un cont inactiv!");
        }

        Card card = new Card();
        card.setAccount(account);
        card.setCardNumber(generateUniqueCardNumber());
        card.setType("DEBIT");

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 3);
        card.setExpirationDate(calendar.getTime());

        card.setCvv(String.format("%03d", new Random().nextInt(1000)));
        card.setHolderName(holderName != null && !holderName.isBlank() ? holderName : "CARDHOLDER");
        card.setStatus("ACTIVE");
        card.setCreatedAt(new Date());
        card.setUpdatedAt(new Date());

        Card saved = cardRepository.save(card);
        log.info("Issued card {} for account {}", saved.getCardId(), accountId);
        return cardMapper.toCardResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponseDTO getCardFromAccount(Integer userId, Long accountId) {
        authorizationService.requireAccount(userId, accountId, ResourceAuthorizationService.AccountPermission.READ);

        Card card = cardRepository.findFirstByAccountAccountIdAndStatus(accountId, "ACTIVE")
                .or(() -> cardRepository.findFirstByAccountAccountId(accountId))
                .orElseThrow(() -> new ResourceNotFoundException("Nu s-a gasit niciun card pentru acest cont"));

        return cardMapper.toCardResponseDTO(card);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardResponseDTO> getCardsForAccount(Integer userId, Long accountId) {
        authorizationService.requireAccount(userId, accountId, ResourceAuthorizationService.AccountPermission.READ);

        List<Card> cards = cardRepository.findByAccountAccountId(accountId);
        return cards.stream().map(cardMapper::toCardResponseDTO).toList();
    }

    @Override
    @Transactional
    public void updateCardStatus(Integer userId, Long accountId, Integer cardId, String status) {
        authorizationService.requireAccount(userId, accountId, ResourceAuthorizationService.AccountPermission.OPERATIONAL);
        authorizationService.requireCard(accountId, cardId);

        String upperStatus = status.toUpperCase();
        if (!upperStatus.equals("ACTIVE") && !upperStatus.equals("BLOCKED") && !upperStatus.equals("CLOSED")) {
            throw new IllegalArgumentException("Status invalid: " + status);
        }

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Cardul cu id " + cardId + " nu a fost gasit"));

        card.setStatus(upperStatus);
        card.setUpdatedAt(new Date());
        cardRepository.save(card);
        log.info("Updated card {} status to {} by userId {}", cardId, upperStatus, userId);
    }

    @Override
    @Transactional
    public void deleteCard(Integer userId, Long accountId, Integer cardId) {
        authorizationService.requireAccount(userId, accountId, ResourceAuthorizationService.AccountPermission.OWNER);
        authorizationService.requireCard(accountId, cardId);

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Cardul cu id " + cardId + " nu a fost gasit"));

        cardRepository.delete(card);
        log.info("Deleted card {} from account {} by userId {}", cardId, accountId, userId);
    }

    private String generateUniqueCardNumber() {
        Random random = new Random();
        String number;
        do {
            number = String.format("%04d%04d%04d%04d",
                    4000 + random.nextInt(1000),
                    random.nextInt(10000),
                    random.nextInt(10000),
                    random.nextInt(10000));
        } while (cardRepository.existsByCardNumber(number));
        return number;
    }
}
