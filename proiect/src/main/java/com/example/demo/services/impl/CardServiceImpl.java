package com.example.demo.services.impl;

import com.example.demo.domain.Account;
import com.example.demo.domain.AccountAccess;
import com.example.demo.domain.Card;
import com.example.demo.domain.User;
import com.example.demo.dto.CardResponseDTO;
import com.example.demo.mappers.CardMapper;
import com.example.demo.repositories.AccountAccessRepository;
import com.example.demo.repositories.AccountRepository;
import com.example.demo.repositories.CardRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final CardMapper cardMapper;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AccountAccessRepository accountAccessRepository;

    @Override
    public CardResponseDTO createCard(Integer userId, Long accountId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        if(!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalArgumentException("User is not active");
        }

        Account account = accountRepository.findById(accountId).orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if(!"ACTIVE".equals(account.getStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }

        AccountAccess accountAccess = getActiveAccountAccess(accountId, userId);
        validateUserCanManageCards(accountAccess);

        if (cardRepository.existsCardByAccountAccountId(accountId)) {
            throw new IllegalArgumentException("A card already exists for this account!");
        }

        Card card = new Card();
        card.setAccount(account);

        String generatedCardNumber =generateCardNumber();
        card.setCardNumber(generatedCardNumber);
        card.setType("DEBIT");

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 10);
        card.setExpirationDate(calendar.getTime());

        String holderName = user.getUsername();
        card.setHolderName(holderName);

        card.setStatus("ACTIVE");
        card.setCreatedAt(new Date());
        card.setUpdatedAt(new Date());

        return cardMapper.toCardResponseDTO(cardRepository.save(card));
    }


    private void blockCard(Integer userId, Long accountId, Integer cardId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        if(!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalArgumentException("User is not active");
        }

        Account account = accountRepository.findById(accountId).orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if(!"ACTIVE".equals(account.getStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }

        AccountAccess accountAccess = getActiveAccountAccess(accountId, userId);
        validateUserCanManageCards(accountAccess);

        Card card = cardRepository.findById(Long.valueOf(cardId)).orElseThrow(() -> new IllegalArgumentException("Card not found!"));

        if (!card.getAccount().getAccountId().equals(accountId)) {
            throw new IllegalArgumentException("Card does not belong to this account!");
        }

        if (!"ACTIVE".equals(card.getStatus())) {
            throw new IllegalArgumentException("Only active cards can be blocked!");
        }

        card.setStatus("BLOCKED");
        card.setUpdatedAt(new Date());

        cardRepository.save(card);
    }

    @Override
    public void updateCard(Integer userId, Long accountId, Integer cardId, String status) {
        if("BLOCKED".equals(status)) {
            blockCard(userId, accountId, cardId);
        } else if("ACTIVE".equals(status)) {
            unblockCard(userId, accountId, cardId);
        } else {
            throw new IllegalArgumentException("Invalid card status!");
        }
    }

    private void unblockCard(Integer userId, Long accountId, Integer cardId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        if(!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalArgumentException("User is not active");
        }

        Account account = accountRepository.findById(accountId).orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if(!"ACTIVE".equals(account.getStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }

        AccountAccess accountAccess = getActiveAccountAccess(accountId, userId);
        validateUserCanManageCards(accountAccess);

        Card card = cardRepository.findById(Long.valueOf(cardId)).orElseThrow(() -> new IllegalArgumentException("Card not found!"));

        if (!card.getAccount().getAccountId().equals(accountId)) {
            throw new IllegalArgumentException("Card does not belong to this account!");
        }

        if (!"BLOCKED".equals(card.getStatus())) {
            throw new IllegalArgumentException("Only blocked cards can be unblocked!");
        }

        card.setStatus("ACTIVE");
        card.setUpdatedAt(new Date());

        cardRepository.save(card);
    }

    private String generateCardNumber() {
        String cardNumber;
        boolean exists;

        do{
            StringBuilder stringBuilder = new StringBuilder("4");

            for(int i = 0; i < 15; i++) {
                int digit = (int)(Math.random() * 10);
                stringBuilder.append(digit);
            }

            cardNumber = stringBuilder.toString();

            exists = cardRepository.existsCardByCardNumber(stringBuilder.toString());

        }while(exists);

        return cardNumber;
    }

    @Override
    public void deleteCard(Integer userId, Long accountId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        if(!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalArgumentException("User is not active");
        }

        Account account = accountRepository.findById(accountId).orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if(!"ACTIVE".equals(account.getStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }

        Card card = cardRepository.findCardByAccountAccountId(accountId).orElseThrow(() -> new IllegalArgumentException("Card not found"));

        card.setStatus("CLOSED");
        card.setUpdatedAt(new Date());
        cardRepository.save(card);

    }

    @Override
    public CardResponseDTO getCardFromAccount(Integer userId, Long accountId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        if(!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalArgumentException("User is not active");
        }

        Account account = accountRepository.findById(accountId).orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if(!"ACTIVE".equals(account.getStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }

        getActiveAccountAccess(accountId, userId);

        Optional<Card> optionalCard = cardRepository.findCardByAccountAccountId(accountId);

        if(optionalCard.isEmpty()) {
            return null;
        }

        return cardMapper.toCardResponseDTO(optionalCard.get());
    }

    private AccountAccess getActiveAccountAccess(Long accountId, Integer userId) {
        return accountAccessRepository
                .findByAccountAccountIdAndUserUserIdAndStatus(accountId, userId, "ACTIVE")
                .orElseThrow(() -> new IllegalArgumentException("User does not have access to this account!"));
    }

    private void validateUserCanManageCards(AccountAccess accountAccess) {
        if (!"OWNER".equals(accountAccess.getAccessRole())) {
            throw new IllegalArgumentException("Only the account owner can manage cards!");
        }
    }
}
