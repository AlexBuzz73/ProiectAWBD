package com.example.accountservice;

import com.example.accountservice.client.UserClient;
import com.example.accountservice.domain.Account;
import com.example.accountservice.domain.AccountAccess;
import com.example.accountservice.domain.BankLimit;
import com.example.accountservice.domain.Card;
import com.example.accountservice.dto.*;
import com.example.accountservice.exceptions.ResourceNotFoundException;
import com.example.accountservice.repositories.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestJwtConfig.class, TestTokenGenerator.class})
@Transactional
class AccountServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TestTokenGenerator tokenGenerator;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountAccessRepository accountAccessRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private BankLimitRepository bankLimitRepository;

    @Autowired
    private UserLimitRepository userLimitRepository;

    @MockitoBean
    private UserClient userClient;

    private String user1Token;
    private String user2Token;
    private String adminToken;

    private static final Integer USER1_ID = 101;
    private static final Integer USER2_ID = 102;
    private static final Integer ADMIN_ID = 999;

    @BeforeEach
    void setUp() {
        user1Token = tokenGenerator.generateToken(USER1_ID, "user1", "user1@test.com", "USER");
        user2Token = tokenGenerator.generateToken(USER2_ID, "user2", "user2@test.com", "USER");
        adminToken = tokenGenerator.generateToken(ADMIN_ID, "admin", "admin@test.com", "ADMIN");

        // Set default bank limit
        bankLimitRepository.deleteAll();
        BankLimit bankLimit = new BankLimit();
        bankLimit.setMaxAmountPerTransactionRon(BigDecimal.valueOf(5000.0));
        bankLimit.setMaxDailyAmountRon(BigDecimal.valueOf(20000.0));
        bankLimit.setMaxDailyTransactionsCount(BigDecimal.valueOf(10.0));
        bankLimit.setStatus("ACTIVE");
        bankLimitRepository.save(bankLimit);
    }

    private Account createTestAccount(String alias, String currency, BigDecimal balance, String status) {
        Account account = new Account();
        account.setAlias(alias);
        account.setCurrency(currency);
        account.setBalance(balance);
        account.setStatus(status);
        account.setIban("RO11BANK" + System.nanoTime());
        return accountRepository.save(account);
    }

    private AccountAccess grantAccess(Account account, Integer userId, String role) {
        AccountAccess access = new AccountAccess();
        access.setAccount(account);
        access.setUserId(userId);
        access.setAccessRole(role);
        access.setStatus("ACTIVE");
        return accountAccessRepository.save(access);
    }

    // ==========================================
    // 1. Authentication & Security Edge Cases
    // ==========================================

    @Test
    @DisplayName("1. Request without Bearer token returns 401 Unauthorized")
    void test1_unauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Autentificare necesara"));
    }

    @Test
    @DisplayName("2. Request with unsupported HTTP method returns 405 Method Not Allowed")
    void test2_unsupportedMethodReturns405() throws Exception {
        mockMvc.perform(post("/api/accounts/summary/currency")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("Metoda HTTP nu este suportata"));
    }

    // ==========================================
    // 2. Account Creation & Listing
    // ==========================================

    @Test
    @DisplayName("3. POST /api/accounts creates account with unique IBAN, 0 balance, and current user as OWNER")
    void test3_createAccountSuccess() throws Exception {
        CreateSingleAccountRequestDTO dto = new CreateSingleAccountRequestDTO("Cont Salariu", "RON", null, 0.0);

        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").isNumber())
                .andExpect(jsonPath("$.alias").value("Cont Salariu"))
                .andExpect(jsonPath("$.currency").value("RON"))
                .andExpect(jsonPath("$.balance").value(0.0))
                .andExpect(jsonPath("$.iban").value(startsWith("RO11BANK")))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        Long accountId = root.get("accountId").asLong();

        // Verify AccountAccess was created for USER1 with OWNER role
        Optional<AccountAccess> access = accountAccessRepository.findByAccountAccountIdAndUserIdAndStatus(accountId, USER1_ID, "ACTIVE");
        assertThat(access).isPresent();
        assertThat(access.get().getAccessRole()).isEqualTo("OWNER");
    }

    @Test
    @DisplayName("4. POST /api/accounts with blank alias returns 400 Bad Request")
    void test4_createAccountBlankAlias() throws Exception {
        CreateSingleAccountRequestDTO dto = new CreateSingleAccountRequestDTO("", "RON", null, 0.0);

        mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Campurile furnizate sunt invalide"));
    }

    @Test
    @DisplayName("5. POST /api/accounts with invalid currency returns 400 Bad Request")
    void test5_createAccountInvalidCurrency() throws Exception {
        CreateSingleAccountRequestDTO dto = new CreateSingleAccountRequestDTO("Cont", "GBP", null, 0.0);

        mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("6. GET /api/accounts returns only current user's accounts with accessRole")
    void test6_getAccountsForCurrentUserOnly() throws Exception {
        Account acc1 = createTestAccount("User1 Main", "RON", BigDecimal.valueOf(100.0), "ACTIVE");
        grantAccess(acc1, USER1_ID, "OWNER");

        Account acc2 = createTestAccount("User2 Main", "EUR", BigDecimal.valueOf(200.0), "ACTIVE");
        grantAccess(acc2, USER2_ID, "OWNER");

        Account accShared = createTestAccount("Shared Account", "RON", BigDecimal.valueOf(50.0), "ACTIVE");
        grantAccess(accShared, USER1_ID, "VIEWER");
        grantAccess(accShared, USER2_ID, "OWNER");

        // User1 should see acc1 (OWNER) and accShared (VIEWER), but NOT acc2
        mockMvc.perform(get("/api/accounts")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.accountId == " + acc1.getAccountId() + ")].accountRole").value("OWNER"))
                .andExpect(jsonPath("$[?(@.accountId == " + accShared.getAccountId() + ")].accountRole").value("VIEWER"))
                .andExpect(jsonPath("$[?(@.accountId == " + acc2.getAccountId() + ")]").doesNotExist());
    }

    // ==========================================
    // 3. Account Details & Authorization Matrix
    // ==========================================

    @Test
    @DisplayName("7. GET /api/accounts/{id} accessible by OWNER with full operational permissions")
    void test7_getAccountDetailsOwner() throws Exception {
        Account acc = createTestAccount("Owner Acc", "RON", BigDecimal.valueOf(500.0), "ACTIVE");
        grantAccess(acc, USER1_ID, "OWNER");

        mockMvc.perform(get("/api/accounts/" + acc.getAccountId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(acc.getAccountId()))
                .andExpect(jsonPath("$.accountRole").value("OWNER"))
                .andExpect(jsonPath("$.canInitiateTransactions").value(true));
    }

    @Test
    @DisplayName("8. GET /api/accounts/{id} accessible by CO_OWNER with operational permissions")
    void test8_getAccountDetailsCoOwner() throws Exception {
        Account acc = createTestAccount("CoOwner Acc", "RON", BigDecimal.valueOf(500.0), "ACTIVE");
        grantAccess(acc, USER1_ID, "CO_OWNER");

        mockMvc.perform(get("/api/accounts/" + acc.getAccountId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountRole").value("CO_OWNER"))
                .andExpect(jsonPath("$.canInitiateTransactions").value(true));
    }

    @Test
    @DisplayName("9. GET /api/accounts/{id} accessible by VIEWER with read-only permissions")
    void test9_getAccountDetailsViewer() throws Exception {
        Account acc = createTestAccount("Viewer Acc", "RON", BigDecimal.valueOf(500.0), "ACTIVE");
        grantAccess(acc, USER1_ID, "VIEWER");

        mockMvc.perform(get("/api/accounts/" + acc.getAccountId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountRole").value("VIEWER"))
                .andExpect(jsonPath("$.canInitiateTransactions").value(false));
    }

    @Test
    @DisplayName("10. GET /api/accounts/{id} by user without access returns 403 Forbidden")
    void test10_getAccountDetailsWithoutAccessReturns403() throws Exception {
        Account acc = createTestAccount("User2 Secret", "RON", BigDecimal.valueOf(1000.0), "ACTIVE");
        grantAccess(acc, USER2_ID, "OWNER");

        mockMvc.perform(get("/api/accounts/" + acc.getAccountId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acces interzis la cont"));
    }

    @Test
    @DisplayName("11. GET /api/accounts/{id} for non-existent account returns 404 Not Found")
    void test11_getNonExistentAccountReturns404() throws Exception {
        mockMvc.perform(get("/api/accounts/999999")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("nu a fost gasit")));
    }

    // ==========================================
    // 4. Account Closing
    // ==========================================

    @Test
    @DisplayName("12. PUT /api/accounts/{id}/close by OWNER with 0 balance closes account")
    void test12_closeAccountSuccess() throws Exception {
        Account acc = createTestAccount("To Close", "RON", BigDecimal.ZERO, "ACTIVE");
        grantAccess(acc, USER1_ID, "OWNER");

        mockMvc.perform(put("/api/accounts/" + acc.getAccountId() + "/close")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk());

        Account updated = accountRepository.findById(acc.getAccountId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("13. PUT /api/accounts/{id}/close by CO_OWNER or VIEWER returns 403 Forbidden")
    void test13_closeAccountNonOwnerForbidden() throws Exception {
        Account acc = createTestAccount("To Close", "RON", BigDecimal.ZERO, "ACTIVE");
        grantAccess(acc, USER1_ID, "CO_OWNER");

        mockMvc.perform(put("/api/accounts/" + acc.getAccountId() + "/close")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("14. PUT /api/accounts/{id}/close when balance > 0 returns 400 Bad Request")
    void test14_closeAccountPositiveBalanceFails() throws Exception {
        Account acc = createTestAccount("With Money", "RON", BigDecimal.valueOf(150.0), "ACTIVE");
        grantAccess(acc, USER1_ID, "OWNER");

        mockMvc.perform(put("/api/accounts/" + acc.getAccountId() + "/close")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Soldul contului trebuie sa fie 0")));
    }

    // ==========================================
    // 5. Card Management
    // ==========================================

    @Test
    @DisplayName("15. POST /api/accounts/{id}/cards by OWNER creates 16-digit card")
    void test15_createCardByOwner() throws Exception {
        Account acc = createTestAccount("Card Acc", "RON", BigDecimal.valueOf(200.0), "ACTIVE");
        grantAccess(acc, USER1_ID, "OWNER");

        MvcResult result = mockMvc.perform(post("/api/accounts/" + acc.getAccountId() + "/cards")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardId").isNumber())
                .andExpect(jsonPath("$.cardNumber").value(matchesRegex("^\\d{16}$")))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.type").value("DEBIT"))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        int cardId = root.get("cardId").asInt();
        Card card = cardRepository.findById(cardId).orElseThrow();
        assertThat(card.getCvv()).matches("^\\d{3}$");
        assertThat(card.getAccount().getAccountId()).isEqualTo(acc.getAccountId());
    }

    @Test
    @DisplayName("16. POST /api/accounts/{id}/cards by CO_OWNER succeeds")
    void test16_createCardByCoOwner() throws Exception {
        Account acc = createTestAccount("Card Acc", "RON", BigDecimal.valueOf(200.0), "ACTIVE");
        grantAccess(acc, USER1_ID, "CO_OWNER");

        mockMvc.perform(post("/api/accounts/" + acc.getAccountId() + "/cards")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("17. POST /api/accounts/{id}/cards by VIEWER returns 403 Forbidden")
    void test17_createCardByViewerForbidden() throws Exception {
        Account acc = createTestAccount("Card Acc", "RON", BigDecimal.valueOf(200.0), "ACTIVE");
        grantAccess(acc, USER1_ID, "VIEWER");

        mockMvc.perform(post("/api/accounts/" + acc.getAccountId() + "/cards")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("18. GET /api/accounts/{id}/cards returns cards for account to OWNER/CO_OWNER/VIEWER")
    void test18_getCardsSuccess() throws Exception {
        Account acc = createTestAccount("Card Acc", "RON", BigDecimal.valueOf(200.0), "ACTIVE");
        grantAccess(acc, USER1_ID, "VIEWER");

        Card card = new Card();
        card.setAccount(acc);
        card.setCardNumber("4000123456789010");
        card.setCvv("123");
        card.setExpirationDate(new Date(System.currentTimeMillis() + 100000000L));
        card.setHolderName("TEST");
        card.setStatus("ACTIVE");
        cardRepository.save(card);

        mockMvc.perform(get("/api/accounts/" + acc.getAccountId() + "/cards")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cardNumber").value("4000123456789010"));
    }

    @Test
    @DisplayName("19. GET /api/accounts/{id}/cards for user without access returns 403 Forbidden")
    void test19_getCardsWithoutAccessForbidden() throws Exception {
        Account acc = createTestAccount("Card Acc", "RON", BigDecimal.valueOf(200.0), "ACTIVE");
        grantAccess(acc, USER2_ID, "OWNER");

        mockMvc.perform(get("/api/accounts/" + acc.getAccountId() + "/cards")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("20. PATCH /api/accounts/{id}/cards/{cardId}/status/{status} updates status (BLOCKED)")
    void test20_updateCardStatusSuccess() throws Exception {
        Account acc = createTestAccount("Card Acc", "RON", BigDecimal.valueOf(200.0), "ACTIVE");
        grantAccess(acc, USER1_ID, "OWNER");

        Card card = new Card();
        card.setAccount(acc);
        card.setCardNumber("4000123456789011");
        card.setCvv("123");
        card.setExpirationDate(new Date());
        card.setHolderName("TEST");
        card.setStatus("ACTIVE");
        Card savedCard = cardRepository.save(card);

        mockMvc.perform(patch("/api/accounts/" + acc.getAccountId() + "/cards/" + savedCard.getCardId() + "/status/BLOCKED")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk());

        Card reloaded = cardRepository.findById(savedCard.getCardId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo("BLOCKED");
    }

    @Test
    @DisplayName("21. PATCH /api/accounts/{id}/cards/{cardId}/status/{status} with invalid status returns 400 Bad Request")
    void test21_updateCardStatusInvalid() throws Exception {
        Account acc = createTestAccount("Card Acc", "RON", BigDecimal.valueOf(200.0), "ACTIVE");
        grantAccess(acc, USER1_ID, "OWNER");

        Card card = new Card();
        card.setAccount(acc);
        card.setCardNumber("4000123456789012");
        card.setCvv("123");
        card.setExpirationDate(new Date());
        card.setHolderName("TEST");
        card.setStatus("ACTIVE");
        Card savedCard = cardRepository.save(card);

        mockMvc.perform(patch("/api/accounts/" + acc.getAccountId() + "/cards/" + savedCard.getCardId() + "/status/INVALID_STATUS")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Status invalid")));
    }

    @Test
    @DisplayName("22. DELETE /api/accounts/{id}/cards/{cardId} by OWNER removes card")
    void test22_deleteCardByOwner() throws Exception {
        Account acc = createTestAccount("Card Acc", "RON", BigDecimal.valueOf(200.0), "ACTIVE");
        grantAccess(acc, USER1_ID, "OWNER");

        Card card = new Card();
        card.setAccount(acc);
        card.setCardNumber("4000123456789013");
        card.setCvv("123");
        card.setExpirationDate(new Date());
        card.setHolderName("TEST");
        card.setStatus("ACTIVE");
        Card savedCard = cardRepository.save(card);

        mockMvc.perform(delete("/api/accounts/" + acc.getAccountId() + "/cards/" + savedCard.getCardId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNoContent());

        assertThat(cardRepository.existsById(savedCard.getCardId())).isFalse();
    }

    @Test
    @DisplayName("23. DELETE /api/accounts/{id}/cards/{cardId} by VIEWER returns 403 Forbidden")
    void test23_deleteCardByViewerForbidden() throws Exception {
        Account acc = createTestAccount("Card Acc", "RON", BigDecimal.valueOf(200.0), "ACTIVE");
        grantAccess(acc, USER1_ID, "VIEWER");

        Card card = new Card();
        card.setAccount(acc);
        card.setCardNumber("4000123456789014");
        card.setCvv("123");
        card.setExpirationDate(new Date());
        card.setHolderName("TEST");
        card.setStatus("ACTIVE");
        Card savedCard = cardRepository.save(card);

        mockMvc.perform(delete("/api/accounts/" + acc.getAccountId() + "/cards/" + savedCard.getCardId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 6. User Limits & Fallback
    // ==========================================

    @Test
    @DisplayName("24. GET /api/limits/me returns bank limits as fallback when user has no custom limits")
    void test24_getLimitsFallbackToBankLimits() throws Exception {
        mockMvc.perform(get("/api/limits/me")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxAmountPerTransactionRon").value(5000.0))
                .andExpect(jsonPath("$.maxDailyAmountRon").value(20000.0))
                .andExpect(jsonPath("$.maxDailyTransactionsCount").value(10.0));
    }

    @Test
    @DisplayName("25. PUT /api/limits/me updates user limits within bank limits")
    void test25_updateUserLimitsSuccess() throws Exception {
        UserLimitRequestDTO dto = new UserLimitRequestDTO(
                BigDecimal.valueOf(2000.0),
                BigDecimal.valueOf(8000.0),
                BigDecimal.valueOf(5.0)
        );

        mockMvc.perform(put("/api/limits/me")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxAmountPerTransactionRon").value(2000.0))
                .andExpect(jsonPath("$.maxDailyAmountRon").value(8000.0))
                .andExpect(jsonPath("$.maxDailyTransactionsCount").value(5.0));

        // Subsequent GET returns the user limit, not bank limit
        mockMvc.perform(get("/api/limits/me")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxAmountPerTransactionRon").value(2000.0));
    }

    @Test
    @DisplayName("26. PUT /api/limits/me exceeding bank limits returns 400 Bad Request")
    void test26_updateUserLimitsExceedingBankLimitsFails() throws Exception {
        UserLimitRequestDTO dto = new UserLimitRequestDTO(
                BigDecimal.valueOf(10000.0), // Bank limit is 5000.0
                BigDecimal.valueOf(8000.0),
                BigDecimal.valueOf(5.0)
        );

        mockMvc.perform(put("/api/limits/me")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("depaseste limita bancii")));
    }

    @Test
    @DisplayName("27. DELETE /api/limits/me resets user limits")
    void test27_deleteUserLimits() throws Exception {
        UserLimitRequestDTO dto = new UserLimitRequestDTO(
                BigDecimal.valueOf(1000.0),
                BigDecimal.valueOf(3000.0),
                BigDecimal.valueOf(3.0)
        );
        mockMvc.perform(put("/api/limits/me")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/limits/me")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNoContent());

        // After delete, falls back to bank limits
        mockMvc.perform(get("/api/limits/me")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxAmountPerTransactionRon").value(5000.0));
    }

    // ==========================================
    // 7. Admin Bank Limits
    // ==========================================

    @Test
    @DisplayName("28. Admin bank-limits endpoints accessible by ADMIN, forbidden for USER")
    void test28_adminBankLimitsRoleAccess() throws Exception {
        // User forbidden
        mockMvc.perform(get("/api/admin/bank-limits")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isForbidden());

        // Admin allowed
        mockMvc.perform(get("/api/admin/bank-limits")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxAmountPerTransactionRon").value(5000.0));

        // Admin updates bank limits
        BankLimitRequestDTO updateDto = new BankLimitRequestDTO(
                BigDecimal.valueOf(8000.0),
                BigDecimal.valueOf(30000.0),
                BigDecimal.valueOf(15.0)
        );

        mockMvc.perform(put("/api/admin/bank-limits")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxAmountPerTransactionRon").value(8000.0));
    }

    // ==========================================
    // 8. Admin Shared Account & Inter-service Communication
    // ==========================================

    @Test
    @DisplayName("29. Admin creates shared account verifying users via user-service client")
    void test29_createSharedAccountSuccess() throws Exception {
        Mockito.when(userClient.requireUserByEmail("user1@test.com"))
                .thenReturn(new UserLookupDTO(USER1_ID, "user1", "user1@test.com", "ROLE_USER", true));
        Mockito.when(userClient.requireUserByEmail("user2@test.com"))
                .thenReturn(new UserLookupDTO(USER2_ID, "user2", "user2@test.com", "ROLE_USER", true));

        SharedAccountRequest dto = new SharedAccountRequest(
                "Cont Comun Familie",
                "RON",
                List.of(
                        new UserRoleDTO("user1@test.com", "OWNER"),
                        new UserRoleDTO("user2@test.com", "VIEWER")
                )
        );

        MvcResult result = mockMvc.perform(post("/api/admin/accounts/shared")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alias").value("Cont Comun Familie"))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        Long accountId = root.get("accountId").asLong();

        // Check user1 is OWNER, user2 is VIEWER
        Optional<AccountAccess> u1Access = accountAccessRepository.findByAccountAccountIdAndUserIdAndStatus(accountId, USER1_ID, "ACTIVE");
        Optional<AccountAccess> u2Access = accountAccessRepository.findByAccountAccountIdAndUserIdAndStatus(accountId, USER2_ID, "ACTIVE");

        assertThat(u1Access).isPresent();
        assertThat(u1Access.get().getAccessRole()).isEqualTo("OWNER");
        assertThat(u2Access).isPresent();
        assertThat(u2Access.get().getAccessRole()).isEqualTo("VIEWER");
    }

    @Test
    @DisplayName("30. Admin shared account creation when user does not exist in user-service returns 404 Not Found")
    void test30_createSharedAccountUserNotFound() throws Exception {
        Mockito.when(userClient.requireUserByEmail("missing@test.com"))
                .thenThrow(new ResourceNotFoundException("Utilizatorul cu email-ul missing@test.com nu a fost gasit"));

        SharedAccountRequest dto = new SharedAccountRequest(
                "Cont Comun",
                "RON",
                List.of(
                        new UserRoleDTO("missing@test.com", "OWNER")
                )
        );

        mockMvc.perform(post("/api/admin/accounts/shared")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("nu a fost gasit")));
    }

    @Test
    @DisplayName("31. Admin revokes user access to account")
    void test31_adminRevokeAccess() throws Exception {
        Account acc = createTestAccount("Shared Acc", "RON", BigDecimal.ZERO, "ACTIVE");
        grantAccess(acc, USER1_ID, "OWNER");
        grantAccess(acc, USER2_ID, "VIEWER");

        Mockito.when(userClient.requireUserByEmail("user2@test.com"))
                .thenReturn(new UserLookupDTO(USER2_ID, "user2", "user2@test.com", "ROLE_USER", true));

        mockMvc.perform(delete("/api/admin/accounts/" + acc.getAccountId() + "/access")
                        .param("email", "user2@test.com")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        Optional<AccountAccess> access = accountAccessRepository.findByAccountAccountIdAndUserIdAndStatus(acc.getAccountId(), USER2_ID, "ACTIVE");
        assertThat(access).isEmpty();
    }

    @Test
    @DisplayName("32. Paged accounts and currency summary endpoints return 200 OK")
    void test32_pagedAndCurrencySummary() throws Exception {
        Account acc1 = createTestAccount("Acc RON", "RON", BigDecimal.valueOf(300.0), "ACTIVE");
        grantAccess(acc1, USER1_ID, "OWNER");

        Account acc2 = createTestAccount("Acc EUR", "EUR", BigDecimal.valueOf(500.0), "ACTIVE");
        grantAccess(acc2, USER1_ID, "OWNER");

        mockMvc.perform(get("/api/accounts/paged?page=0&size=5")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/accounts/summary/currency")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }
}
