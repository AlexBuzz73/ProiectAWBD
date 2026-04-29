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
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        if(!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("User is not active");
        }

        Account account = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Account not found"));

        if(!"ACTIVE".equals(account.getStatus())) {
            throw new RuntimeException("Account is not active");
        }

        AccountAccess accountAccess = accountAccessRepository.findById(accountId).orElseThrow(() -> new RuntimeException("User doesn't have acces to this acccount!"));

        if("VIEWER".equals(accountAccess.getAccessRole())) {
            throw new RuntimeException("Viewer user cannot create cards!");
        }

        boolean activeCardExists = cardRepository.existsCardByAccountIdAndStatus(accountId, "ACTIVE");

        if(activeCardExists) {
            throw new RuntimeException("Card already exists for this account!");
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

        card.setStatus(card.getStatus());

        return cardMapper.toCardResponseDTO(cardRepository.save(card));
    }

    @Override
    public void blockCard(Integer userId, Long accountId, Integer cardId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        if(!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("User is not active");
        }

        Account account = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Account not found"));

        if(!"ACTIVE".equals(account.getStatus())) {
            throw new RuntimeException("Account is not active");
        }

        Card card = cardRepository.findCardByAccountIdAndStatus(accountId, "ACTIVE").orElseThrow(() -> new RuntimeException("Card not found"));

        card.setStatus("BLOCKED");

    }

    @Override
    public void unblockCard(Integer userId, Long accountId, Integer cardId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        if(!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("User is not active");
        }

        Account account = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Account not found"));

        if(!"ACTIVE".equals(account.getStatus())) {
            throw new RuntimeException("Account is not active");
        }

        Card card = cardRepository.findCardByAccountIdAndStatus(accountId, "BLOCKED").orElseThrow(() -> new RuntimeException("Card not found"));

        card.setStatus("ACTIVE");
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
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        if(!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("User is not active");
        }

        Account account = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Account not found"));

        if(!"ACTIVE".equals(account.getStatus())) {
            throw new RuntimeException("Account is not active");
        }

        Card card = cardRepository.findCardByAccountIdAndStatus(accountId, "ACTIVE").orElseThrow(() -> new RuntimeException("Card not found"));

        card.setStatus("CLOSED");

    }

    @Override
    public CardResponseDTO getActiveCardFromAccount(Integer userId, Long accountId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        if(!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("User is not active");
        }

        Account account = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Account not found"));

        if(!"ACTIVE".equals(account.getStatus())) {
            throw new RuntimeException("Account is not active");
        }

        Card card = cardRepository.findCardByAccountIdAndStatus(accountId, "ACTIVE").orElseThrow(() -> new RuntimeException("Card not found"));

        return cardMapper.toCardResponseDTO(card);
    }
}
