package com.example.demo.integration;

import com.example.demo.domain.Account;
import com.example.demo.domain.AccountAccess;
import com.example.demo.domain.Card;
import com.example.demo.domain.User;
import com.example.demo.repositories.AccountAccessRepository;
import com.example.demo.repositories.AccountRepository;
import com.example.demo.repositories.CardRepository;
import com.example.demo.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Calendar;
import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountAccessRepository accountAccessRepository;

    @Autowired
    private CardRepository cardRepository;

    private User activeUser;
    private User viewerUser;
    private Account activeAccount;
    private Account closedAccount;
    private Card activeCard;
    private Card blockedCard;

    @BeforeEach
    void setUp() {
        cardRepository.deleteAll();
        accountAccessRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        activeUser = new User();
        activeUser.setUsername("teo");
        activeUser.setEmail("teo@test.com");
        activeUser.setPasswordHash("password");
        activeUser.setRole("USER");
        activeUser.setStatus("ACTIVE");
        activeUser.setFailedLoginAttempts(0);
        activeUser.setCreatedAt(new Date());
        activeUser.setUpdatedAt(new Date());
        activeUser = userRepository.save(activeUser);

        viewerUser = new User();
        viewerUser.setUsername("viewer");
        viewerUser.setEmail("viewer@test.com");
        viewerUser.setPasswordHash("password");
        viewerUser.setRole("USER");
        viewerUser.setStatus("ACTIVE");
        viewerUser.setFailedLoginAttempts(0);
        viewerUser.setCreatedAt(new Date());
        viewerUser.setUpdatedAt(new Date());
        viewerUser = userRepository.save(viewerUser);

        activeAccount = new Account();
        activeAccount.setIban("RO49AAAA1B31007593840000");
        activeAccount.setCurrency("RON");
        activeAccount.setBalance(1000);
        activeAccount.setAlias("Main account");
        activeAccount.setStatus("ACTIVE");
        activeAccount.setCreatedAt(new Date());
        activeAccount.setUpdatedAt(new Date());
        activeAccount = accountRepository.save(activeAccount);

        closedAccount = new Account();
        closedAccount.setIban("RO49AAAA1B31007593840001");
        closedAccount.setCurrency("RON");
        closedAccount.setBalance(1000);
        closedAccount.setAlias("Closed account");
        closedAccount.setStatus("CLOSED");
        closedAccount.setCreatedAt(new Date());
        closedAccount.setUpdatedAt(new Date());
        closedAccount = accountRepository.save(closedAccount);

        AccountAccess ownerAccess = new AccountAccess();
        ownerAccess.setUser(activeUser);
        ownerAccess.setAccount(activeAccount);
        ownerAccess.setAccessRole("OWNER");
        ownerAccess.setStatus("ACTIVE");
        ownerAccess.setCreatedAt(new Date());
        ownerAccess.setUpdatedAt(new Date());
        accountAccessRepository.save(ownerAccess);

        AccountAccess viewerAccess = new AccountAccess();
        viewerAccess.setUser(viewerUser);
        viewerAccess.setAccount(activeAccount);
        viewerAccess.setAccessRole("VIEWER");
        viewerAccess.setStatus("ACTIVE");
        viewerAccess.setCreatedAt(new Date());
        viewerAccess.setUpdatedAt(new Date());
        accountAccessRepository.save(viewerAccess);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 10);

        activeCard = new Card();
        activeCard.setAccount(activeAccount);
        activeCard.setCardNumber("4123456789012345");
        activeCard.setType("DEBIT");
        activeCard.setExpirationDate(calendar.getTime());
        activeCard.setHolderName("teo");
        activeCard.setStatus("ACTIVE");
        activeCard.setCreatedAt(new Date());
        activeCard.setUpdatedAt(new Date());
        activeCard = cardRepository.save(activeCard);

        blockedCard = new Card();
        blockedCard.setAccount(activeAccount);
        blockedCard.setCardNumber("4987654321098765");
        blockedCard.setType("DEBIT");
        blockedCard.setExpirationDate(calendar.getTime());
        blockedCard.setHolderName("teo");
        blockedCard.setStatus("BLOCKED");
        blockedCard.setCreatedAt(new Date());
        blockedCard.setUpdatedAt(new Date());
        blockedCard = cardRepository.save(blockedCard);
    }

    @Test
    void getActiveCardForAccount_shouldReturnActiveCard() throws Exception {
        mockMvc.perform(get("/api/users/" + activeUser.getUserId() + "/accounts/" + activeAccount.getAccountId() + "/card"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber", is("4123456789012345")))
                .andExpect(jsonPath("$.type", is("DEBIT")))
                .andExpect(jsonPath("$.holderName", is("teo")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    void createCard_shouldReturnBadRequest_whenActiveCardAlreadyExists() throws Exception {
        mockMvc.perform(post("/api/users/" + activeUser.getUserId() + "/accounts/" + activeAccount.getAccountId() + "/card"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCard_shouldCreateCard_whenNoActiveCardExists() throws Exception {
        cardRepository.delete(activeCard);

        mockMvc.perform(post("/api/users/" + activeUser.getUserId() + "/accounts/" + activeAccount.getAccountId() + "/card"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber", notNullValue()))
                .andExpect(jsonPath("$.type", is("DEBIT")))
                .andExpect(jsonPath("$.holderName", is("teo")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    void createCard_shouldReturnBadRequest_whenUserIsViewer() throws Exception {
        cardRepository.delete(activeCard);

        mockMvc.perform(post("/api/users/" + viewerUser.getUserId() + "/accounts/" + activeAccount.getAccountId() + "/card"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCard_shouldReturnBadRequest_whenAccountIsClosed() throws Exception {
        mockMvc.perform(post("/api/users/" + activeUser.getUserId() + "/accounts/" + closedAccount.getAccountId() + "/card"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCard_shouldBlockActiveCard() throws Exception {
        mockMvc.perform(patch("/api/users/" + activeUser.getUserId()
                        + "/accounts/" + activeAccount.getAccountId()
                        + "/card/" + activeCard.getCardId()
                        + "/status/ACTIVE"))
                .andExpect(status().isOk());

        Card updatedCard = cardRepository.findById((long) activeCard.getCardId())
                .orElseThrow();

        org.junit.jupiter.api.Assertions.assertEquals("BLOCKED", updatedCard.getStatus());
    }

    @Test
    void updateCard_shouldUnblockBlockedCard() throws Exception {
        cardRepository.delete(activeCard);

        mockMvc.perform(patch("/api/users/" + activeUser.getUserId()
                        + "/accounts/" + activeAccount.getAccountId()
                        + "/card/" + blockedCard.getCardId()
                        + "/status/BLOCKED"))
                .andExpect(status().isOk());

        Card updatedCard = cardRepository.findById((long) blockedCard.getCardId())
                .orElseThrow();

        org.junit.jupiter.api.Assertions.assertEquals("ACTIVE", updatedCard.getStatus());
    }

    @Test
    void deleteCard_shouldCloseActiveCard() throws Exception {
        mockMvc.perform(delete("/api/users/" + activeUser.getUserId()
                        + "/accounts/" + activeAccount.getAccountId()
                        + "/card/" + activeCard.getCardId()
                        + "/delete"))
                .andExpect(status().isOk());

        Card deletedCard = cardRepository.findById((long) activeCard.getCardId())
                .orElseThrow();

        org.junit.jupiter.api.Assertions.assertEquals("CLOSED", deletedCard.getStatus());
    }
}