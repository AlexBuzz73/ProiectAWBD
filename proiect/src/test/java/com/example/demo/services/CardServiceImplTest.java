package com.example.demo.services;

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
import com.example.demo.services.impl.CardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    @Mock
    private CardMapper cardMapper;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountAccessRepository accountAccessRepository;

    @InjectMocks
    private CardServiceImpl cardService;

    private User user;
    private Account account;
    private AccountAccess ownerAccess;
    private AccountAccess viewerAccess;
    private Card activeCard;
    private Card blockedCard;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1);
        user.setUsername("teo");
        user.setStatus("ACTIVE");

        account = new Account();
        account.setAccountId(10L);
        account.setStatus("ACTIVE");
        account.setAlias("Main account");

        ownerAccess = new AccountAccess();
        ownerAccess.setAccountAccessId(1L);
        ownerAccess.setUser(user);
        ownerAccess.setAccount(account);
        ownerAccess.setAccessRole("OWNER");
        ownerAccess.setStatus("ACTIVE");

        viewerAccess = new AccountAccess();
        viewerAccess.setAccountAccessId(2L);
        viewerAccess.setUser(user);
        viewerAccess.setAccount(account);
        viewerAccess.setAccessRole("VIEWER");
        viewerAccess.setStatus("ACTIVE");

        activeCard = new Card();
        activeCard.setCardId(1);
        activeCard.setAccount(account);
        activeCard.setCardNumber("4123456789012345");
        activeCard.setType("DEBIT");
        activeCard.setHolderName("teo");
        activeCard.setExpirationDate(new Date());
        activeCard.setStatus("ACTIVE");

        blockedCard = new Card();
        blockedCard.setCardId(2);
        blockedCard.setAccount(account);
        blockedCard.setCardNumber("4987654321098765");
        blockedCard.setType("DEBIT");
        blockedCard.setHolderName("teo");
        blockedCard.setExpirationDate(new Date());
        blockedCard.setStatus("BLOCKED");
    }

    @Test
    void createCard_shouldCreateCard_whenUserAndAccountAreValid() {
        CardResponseDTO responseDTO = new CardResponseDTO(
                "4123456789012345",
                "DEBIT",
                activeCard.getExpirationDate(),
                "teo",
                "ACTIVE"
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(accountAccessRepository.findByAccountAccountIdAndUserUserIdAndStatus(10L, 1, "ACTIVE"))
                .thenReturn(Optional.of(ownerAccess));
        when(cardRepository.existsCardByAccountAccountIdAndStatus(10L, "ACTIVE"))
                .thenReturn(false);
        when(cardRepository.existsCardByCardNumber(anyString())).thenReturn(false);
        when(cardRepository.save(any(Card.class))).thenReturn(activeCard);
        when(cardMapper.toCardResponseDTO(activeCard)).thenReturn(responseDTO);

        CardResponseDTO result = cardService.createCard(1, 10L);

        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
        assertEquals("DEBIT", result.getType());

        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void createCard_shouldThrowException_whenUserNotFound() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cardService.createCard(1, 10L)
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void createCard_shouldThrowException_whenUserIsInactive() {
        user.setStatus("INACTIVE");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cardService.createCard(1, 10L)
        );

        assertEquals("User is not active", exception.getMessage());
    }

    @Test
    void createCard_shouldThrowException_whenAccountIsInactive() {
        account.setStatus("CLOSED");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cardService.createCard(1, 10L)
        );

        assertEquals("Account is not active", exception.getMessage());
    }

    @Test
    void createCard_shouldThrowException_whenUserIsViewer() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(accountAccessRepository.findByAccountAccountIdAndUserUserIdAndStatus(10L, 1, "ACTIVE"))
                .thenReturn(Optional.of(viewerAccess));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cardService.createCard(1, 10L)
        );

        assertEquals("Viewer user cannot create cards!", exception.getMessage());
    }

    @Test
    void createCard_shouldThrowException_whenActiveCardAlreadyExists() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(accountAccessRepository.findByAccountAccountIdAndUserUserIdAndStatus(10L, 1, "ACTIVE"))
                .thenReturn(Optional.of(ownerAccess));
        when(cardRepository.existsCardByAccountAccountIdAndStatus(10L, "ACTIVE"))
                .thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cardService.createCard(1, 10L)
        );

        assertEquals("Card already exists for this account!", exception.getMessage());
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void getActiveCardFromAccount_shouldReturnCard_whenCardExists() {
        CardResponseDTO responseDTO = new CardResponseDTO(
                activeCard.getCardNumber(),
                activeCard.getType(),
                activeCard.getExpirationDate(),
                activeCard.getHolderName(),
                activeCard.getStatus()
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(cardRepository.findCardByAccountAccountIdAndStatus(10L, "ACTIVE"))
                .thenReturn(Optional.of(activeCard));
        when(cardMapper.toCardResponseDTO(activeCard)).thenReturn(responseDTO);

        CardResponseDTO result = cardService.getActiveCardFromAccount(1, 10L);

        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
        assertEquals("4123456789012345", result.getCardNumber());
    }

    @Test
    void getActiveCardFromAccount_shouldThrowException_whenCardNotFound() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(cardRepository.findCardByAccountAccountIdAndStatus(10L, "ACTIVE"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cardService.getActiveCardFromAccount(1, 10L)
        );

        assertEquals("Card not found", exception.getMessage());
    }

    @Test
    void updateCard_shouldBlockActiveCard_whenStatusIsActive() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(cardRepository.findCardByAccountAccountIdAndStatus(10L, "ACTIVE"))
                .thenReturn(Optional.of(activeCard));

        cardService.updateCard(1, 10L, 1, "ACTIVE");

        assertEquals("BLOCKED", activeCard.getStatus());
    }

    @Test
    void updateCard_shouldUnblockBlockedCard_whenStatusIsBlocked() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(cardRepository.findCardByAccountAccountIdAndStatus(10L, "BLOCKED"))
                .thenReturn(Optional.of(blockedCard));

        cardService.updateCard(1, 10L, 2, "BLOCKED");

        assertEquals("ACTIVE", blockedCard.getStatus());
    }

    @Test
    void deleteCard_shouldCloseActiveCard() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(cardRepository.findCardByAccountAccountIdAndStatus(10L, "ACTIVE"))
                .thenReturn(Optional.of(activeCard));

        cardService.deleteCard(1, 10L);

        assertEquals("CLOSED", activeCard.getStatus());
    }
}