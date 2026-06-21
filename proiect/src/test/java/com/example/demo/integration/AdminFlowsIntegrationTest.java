package com.example.demo.integration;

import com.example.demo.domain.Account;
import com.example.demo.domain.AccountAccess;
import com.example.demo.domain.User;
import com.example.demo.repositories.AccountAccessRepository;
import com.example.demo.repositories.AccountRepository;
import com.example.demo.repositories.CardRepository;
import com.example.demo.repositories.CategoryRepository;
import com.example.demo.repositories.ExchangeRateRepository;
import com.example.demo.repositories.ScheduledPaymentRepository;
import com.example.demo.repositories.TransactionRepository;
import com.example.demo.repositories.UserLimitRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teste de integrare end-to-end pentru flux 10 (administrativ): deblocare utilizator,
 * creare cont partajat, revocare acces. Bank limits (GET/PUT) sunt deja acoperite de
 * LimitControllerIntegrationTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminFlowsIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private AccountAccessRepository accountAccessRepository;
    @Autowired private CardRepository cardRepository;
    @Autowired private ScheduledPaymentRepository scheduledPaymentRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UserLimitRepository userLimitRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ExchangeRateRepository exchangeRateRepository;

    private User blockedUser;
    private User ownerUser;
    private User coOwnerUser;

    @BeforeEach
    void setUp() {
        // Baza H2 e impartita intre TOATE clasele de integrare dintr-o rulare de teste.
        // Curatam tot ce ar putea referinta accounts/users, nu doar ce folosim noi direct,
        // altfel deleteAllInBatch() de mai jos pica cu eroare de constrangere FK daca o
        // alta clasa (ex. AccountIntegrationTest, CardControllerIntegrationTest) a rulat
        // inaintea noastra si a lasat date in urma.
        cardRepository.deleteAllInBatch();
        scheduledPaymentRepository.deleteAllInBatch();
        transactionRepository.deleteAllInBatch();
        accountAccessRepository.deleteAllInBatch();
        userLimitRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        exchangeRateRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        blockedUser = createUser("blocat", "blocat@test.com", "BLOCKED", 3);
        ownerUser = createUser("owner", "owner@test.com", "ACTIVE", 0);
        coOwnerUser = createUser("coowner", "coowner@test.com", "ACTIVE", 0);
    }

    private User createUser(String username, String email, String status, int failedAttempts) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("hashed-password");
        user.setRole("USER");
        user.setStatus(status);
        user.setFailedLoginAttempts(failedAttempts);
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());
        return userRepository.save(user);
    }

    private Account createAccount(String iban) {
        Account account = new Account();
        account.setIban(iban);
        account.setCurrency("RON");
        account.setBalance(0.0);
        account.setAlias("Cont partajat");
        account.setStatus("ACTIVE");
        account.setCreatedAt(new Date());
        account.setUpdatedAt(new Date());
        return accountRepository.save(account);
    }

    private AccountAccess grantAccess(User user, Account account, String role) {
        AccountAccess access = new AccountAccess();
        access.setUser(user);
        access.setAccount(account);
        access.setAccessRole(role);
        access.setStatus("ACTIVE");
        access.setCreatedAt(new Date());
        access.setUpdatedAt(new Date());
        return accountAccessRepository.save(access);
    }

    // ---------- deblocare utilizator ----------

    @Test
    void unlockUserByEmail_blockedUser_resetsStatusAndFailedAttempts() throws Exception {
        mockMvc.perform(post("/api/admin/unlock-user")
                        .param("email", blockedUser.getEmail()))
                .andExpect(status().isOk());

        User updated = userRepository.findById(blockedUser.getUserId()).orElseThrow();
        assertEquals("ACTIVE", updated.getStatus());
        assertEquals(0, updated.getFailedLoginAttempts());
    }

    @Test
    void unlockUserByEmail_unknownEmail_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/unlock-user")
                        .param("email", "nu-exista@test.com"))
                .andExpect(status().isBadRequest());
    }

    // ---------- creare cont partajat ----------

    @Test
    void createSharedAccount_validRequest_createsAccountWithGeneratedIbanAndAccess() throws Exception {
        String json = """
                {
                  "alias": "Cont familie",
                  "currency": "RON",
                  "users": [
                    {"email": "%s", "role": "OWNER"},
                    {"email": "%s", "role": "CO_OWNER"}
                  ]
                }
                """.formatted(ownerUser.getEmail(), coOwnerUser.getEmail());

        mockMvc.perform(post("/api/admin/create-shared-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alias").value("Cont familie"))
                .andExpect(jsonPath("$.currency").value("RON"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.iban").isNotEmpty());

        Account created = accountRepository.findAll().stream()
                .filter(a -> "Cont familie".equals(a.getAlias()))
                .findFirst()
                .orElseThrow();

        long accessCount = accountAccessRepository.findByAccountAccountId(created.getAccountId()).size();
        assertEquals(2, accessCount);
    }

    @Test
    void createSharedAccount_noOwnerInList_returnsBadRequest() throws Exception {
        String json = """
                {
                  "alias": "Fara owner",
                  "currency": "RON",
                  "users": [
                    {"email": "%s", "role": "CO_OWNER"}
                  ]
                }
                """.formatted(coOwnerUser.getEmail());

        mockMvc.perform(post("/api/admin/create-shared-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    // ---------- revocare acces ----------

    @Test
    void revokeAccountAccess_coOwnerWithAnotherOwnerPresent_setsAccessInactive() throws Exception {
        Account sharedAccount = createAccount("RO49AAAA0000000000000010");
        grantAccess(ownerUser, sharedAccount, "OWNER");
        AccountAccess coOwnerAccess = grantAccess(coOwnerUser, sharedAccount, "CO_OWNER");

        mockMvc.perform(delete("/api/admin/accounts/" + sharedAccount.getAccountId() + "/access")
                        .param("email", coOwnerUser.getEmail()))
                .andExpect(status().isOk());

        AccountAccess updated = accountAccessRepository.findById(coOwnerAccess.getAccountAccessId()).orElseThrow();
        assertEquals("INACTIVE", updated.getStatus());
    }

    @Test
    void revokeAccountAccess_lastActiveOwner_returnsBadRequestAndKeepsAccessActive() throws Exception {
        Account sharedAccount = createAccount("RO49AAAA0000000000000011");
        AccountAccess soleOwnerAccess = grantAccess(ownerUser, sharedAccount, "OWNER");

        mockMvc.perform(delete("/api/admin/accounts/" + sharedAccount.getAccountId() + "/access")
                        .param("email", ownerUser.getEmail()))
                .andExpect(status().isBadRequest());

        AccountAccess unchanged = accountAccessRepository.findById(soleOwnerAccess.getAccountAccessId()).orElseThrow();
        assertEquals("ACTIVE", unchanged.getStatus());
    }
}
