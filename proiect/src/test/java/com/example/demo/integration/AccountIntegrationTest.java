package com.example.demo.integration;

import com.example.demo.domain.Account;
import com.example.demo.domain.User;
import com.example.demo.repositories.AccountAccessRepository;
import com.example.demo.repositories.AccountRepository;
import com.example.demo.repositories.TransactionRepository;
import com.example.demo.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AccountIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountAccessRepository accountAccessRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAllInBatch();
        accountAccessRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void createAccount_thenListAndGetDetails_shouldReturnCorrectAccountData() throws Exception {
        User user = createActiveUser();
        String createAccountJson = """
                {
                  "alias": "Cont principal",
                  "currency": "RON",
                  "externalIban": "RO49AAAA1B31007593840000",
                  "initialAmount": 1000
                }
                """;

        mockMvc.perform(post("/api/accounts").param("userId", String.valueOf(user.getUserId()))
               .contentType(MediaType.APPLICATION_JSON)
               .content(createAccountJson))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.alias").value("Cont principal"))
               .andExpect(jsonPath("$.currency").value("RON"))
               .andExpect(jsonPath("$.balance").value(1000.0))
               .andExpect(jsonPath("$.status").value("ACTIVE"))
               .andReturn()
               .getResponse()
               .getContentAsString();

        assertEquals(1, accountRepository.count());
        assertEquals(1, accountAccessRepository.count());
        assertEquals(1, transactionRepository.count());

        Account savedAccount = accountRepository.findAll().get(0);
        assertEquals("ACTIVE", savedAccount.getStatus());
        assertEquals(1000.0, savedAccount.getBalance());

        mockMvc.perform(get("/api/accounts").param("userId", String.valueOf(user.getUserId())))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].accountId").value(savedAccount.getAccountId()))
               .andExpect(jsonPath("$[0].alias").value("Cont principal"))
               .andExpect(jsonPath("$[0].currency").value("RON"))
               .andExpect(jsonPath("$[0].balance").value(1000.0))
               .andExpect(jsonPath("$[0].accountRole").value("OWNER"));

        mockMvc.perform(get("/api/accounts/{accountId}", savedAccount.getAccountId()).param("userId", String.valueOf(user.getUserId())))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.accountId").value(savedAccount.getAccountId()))
               .andExpect(jsonPath("$.alias").value("Cont principal"))
               .andExpect(jsonPath("$.currency").value("RON"))
               .andExpect(jsonPath("$.balance").value(1000.0))
               .andExpect(jsonPath("$.status").value("ACTIVE"))
               .andExpect(jsonPath("$.accountRole").value("OWNER"))
               .andExpect(jsonPath("$.canInitiateTransactions").value(true));
    }

    @Test
    void closeAccount_shouldRespectBalanceRuleAndHideClosedAccountFromDashboard() throws Exception {
        User user = createActiveUser();
        String createAccountJson = """
                {
                  "alias": "Cont economii",
                  "currency": "RON",
                  "externalIban": "RO49AAAA1B31007593840000",
                  "initialAmount": 500
                }
                """;

        mockMvc.perform(post("/api/accounts").param("userId", String.valueOf(user.getUserId()))
               .contentType(MediaType.APPLICATION_JSON)
               .content(createAccountJson))
               .andExpect(status().isOk());

        Account savedAccount = accountRepository.findAll().get(0);

        mockMvc.perform(put("/api/accounts/{accountId}/close", savedAccount.getAccountId()).param("userId", String.valueOf(user.getUserId())))
               .andExpect(status().isBadRequest())
               .andExpect(content().string("Account balance must be 0 to close the account!"));

        savedAccount.setBalance(0.0);
        accountRepository.save(savedAccount);

        mockMvc.perform(put("/api/accounts/{accountId}/close", savedAccount.getAccountId()).param("userId", String.valueOf(user.getUserId())))
               .andExpect(status().isOk());

        Account closedAccount = accountRepository.findById(savedAccount.getAccountId()).orElseThrow();
        assertEquals("CLOSED", closedAccount.getStatus());

        mockMvc.perform(get("/api/accounts").param("userId", String.valueOf(user.getUserId())))
               .andExpect(status().isOk())
               .andExpect(content().json("[]"));
    }

    private User createActiveUser() {
        User user = new User();
        user.setUsername("account_user");
        user.setEmail("account@test.com");
        user.setPasswordHash("password");
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());
        return userRepository.save(user);
    }
}