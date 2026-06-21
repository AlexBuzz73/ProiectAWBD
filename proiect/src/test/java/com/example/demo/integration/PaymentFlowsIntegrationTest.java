package com.example.demo.integration;

import com.example.demo.domain.*;
import com.example.demo.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teste de integrare end-to-end pentru fluxurile 7 (plati), 8 (transfer intre conturi proprii)
 * si 9 (schimb valutar). Folosesc contextul Spring real, baza H2 de test si trec efectiv prin
 * MockMvc -> controller -> service -> repository -> serializare JSON, nu doar prin service izolat
 * cu mock-uri (cum fac testele unitare).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentFlowsIntegrationTest {

    private static final String PASSWORD = "secret123";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private AccountAccessRepository accountAccessRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private BankLimitRepository bankLimitRepository;
    @Autowired private ExchangeRateRepository exchangeRateRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private ScheduledPaymentRepository scheduledPaymentRepository;
    @Autowired private CardRepository cardRepository;
    @Autowired private UserLimitRepository userLimitRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User userA;
    private User userB;
    private Account accountA1;
    private Account accountA2;
    private Account accountAUsd;
    private Account accountB1;
    private Category category;

    @BeforeEach
    void setUp() {
        // Baza H2 e impartita intre TOATE clasele de integrare dintr-o rulare de teste,
        // de-asta curatam si tabele pe care nu le folosim noi direct (cards, user_limits) -
        // pot avea date ramase de la alta clasa care a rulat inaintea noastra.
        cardRepository.deleteAllInBatch();
        scheduledPaymentRepository.deleteAllInBatch();
        transactionRepository.deleteAllInBatch();
        accountAccessRepository.deleteAllInBatch();
        userLimitRepository.deleteAllInBatch();
        exchangeRateRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        bankLimitRepository.deleteAllInBatch();

        userA = createUser("ana", "ana@test.com");
        userB = createUser("bogdan", "bogdan@test.com");

        accountA1 = createAccount("RO49AAAA0000000000000001", "RON", 1000.0, "Cont principal");
        accountA2 = createAccount("RO49AAAA0000000000000002", "RON", 500.0, "Cont economii");
        accountAUsd = createAccount("RO49AAAA0000000000000003", "USD", 0.0, "Cont USD");
        accountB1 = createAccount("RO49AAAA0000000000000004", "RON", 300.0, "Cont Bogdan");

        grantAccess(userA, accountA1, "OWNER");
        grantAccess(userA, accountA2, "OWNER");
        grantAccess(userA, accountAUsd, "OWNER");
        grantAccess(userB, accountB1, "OWNER");

        category = new Category();
        category.setName("Utilities");
        category.setIsSystem("Y");
        category.setStatus("ACTIVE");
        category.setCreatedAt(new Date());
        category.setUpdatedAt(new Date());
        category = categoryRepository.save(category);

        BankLimit bankLimit = new BankLimit();
        bankLimit.setMaxAmountPerTransactionRon(new BigDecimal("5000"));
        bankLimit.setMaxDailyAmountRon(new BigDecimal("20000"));
        bankLimit.setMaxDailyTransactionsCount(new BigDecimal("50"));
        bankLimit.setStatus("ACTIVE");
        bankLimit.setCreatedAt(new Date());
        bankLimit.setUpdatedAt(new Date());
        bankLimitRepository.save(bankLimit);

        ExchangeRate usdToRon = new ExchangeRate();
        usdToRon.setCurrencyFrom("USD");
        usdToRon.setCurrencyTo("RON");
        usdToRon.setRate(4.5);
        usdToRon.setRateDate(new Date());
        usdToRon.setSource("BNR");
        usdToRon.setCreatedAt(new Date());
        exchangeRateRepository.save(usdToRon);
    }

    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());
        return userRepository.save(user);
    }

    private Account createAccount(String iban, String currency, double balance, String alias) {
        Account account = new Account();
        account.setIban(iban);
        account.setCurrency(currency);
        account.setBalance(balance);
        account.setAlias(alias);
        account.setStatus("ACTIVE");
        account.setCreatedAt(new Date());
        account.setUpdatedAt(new Date());
        return accountRepository.save(account);
    }

    private void grantAccess(User user, Account account, String role) {
        AccountAccess access = new AccountAccess();
        access.setUser(user);
        access.setAccount(account);
        access.setAccessRole(role);
        access.setStatus("ACTIVE");
        access.setCreatedAt(new Date());
        access.setUpdatedAt(new Date());
        accountAccessRepository.save(access);
    }

    // ---------- Flux 7: plati ----------

    @Test
    void initiatePayment_urgentExternalPayment_executesImmediatelyAndDeductsBalance() throws Exception {
        String json = """
                {
                  "sourceAccountId": %d,
                  "destinationIban": "RO49EXTR0000000000009999",
                  "amount": 150,
                  "currency": "RON",
                  "categoryId": %d,
                  "processingType": "URGENT",
                  "description": "Factura curent",
                  "password": "%s"
                }
                """.formatted(accountA1.getAccountId(), category.getCategoryId(), PASSWORD);

        mockMvc.perform(post("/api/payments/initiate")
                        .param("userId", String.valueOf(userA.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.transactionType").value("EXTERNAL"))
                .andExpect(jsonPath("$.amount").value(150.0));

        Account updated = accountRepository.findById(accountA1.getAccountId()).orElseThrow();
        assertEqualsBalance(850.0, updated.getBalance());
    }

    @Test
    void initiatePayment_wrongPassword_returnsBadRequest() throws Exception {
        String json = """
                {
                  "sourceAccountId": %d,
                  "destinationIban": "RO49EXTR0000000000009999",
                  "amount": 100,
                  "currency": "RON",
                  "categoryId": %d,
                  "processingType": "STANDARD",
                  "password": "wrong-password"
                }
                """.formatted(accountA1.getAccountId(), category.getCategoryId());

        mockMvc.perform(post("/api/payments/initiate")
                        .param("userId", String.valueOf(userA.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void initiatePayment_invalidDestinationIban_returnsBadRequest() throws Exception {
        String json = """
                {
                  "sourceAccountId": %d,
                  "destinationIban": "not-an-iban",
                  "amount": 100,
                  "currency": "RON",
                  "categoryId": %d,
                  "processingType": "STANDARD",
                  "password": "%s"
                }
                """.formatted(accountA1.getAccountId(), category.getCategoryId(), PASSWORD);

        mockMvc.perform(post("/api/payments/initiate")
                        .param("userId", String.valueOf(userA.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    // ---------- Flux 8: transfer intre conturi proprii ----------

    @Test
    void transferOwnAccounts_success_movesMoneyBetweenOwnAccounts() throws Exception {
        String json = """
                {
                  "sourceAccountId": %d,
                  "destinationAccountId": %d,
                  "amount": 200,
                  "categoryId": %d,
                  "description": "Economii",
                  "password": "%s"
                }
                """.formatted(accountA1.getAccountId(), accountA2.getAccountId(), category.getCategoryId(), PASSWORD);

        mockMvc.perform(post("/api/payments/transfer-own")
                        .param("userId", String.valueOf(userA.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.transactionType").value("INTERNAL"));

        assertEqualsBalance(800.0, accountRepository.findById(accountA1.getAccountId()).orElseThrow().getBalance());
        assertEqualsBalance(700.0, accountRepository.findById(accountA2.getAccountId()).orElseThrow().getBalance());
    }

    @Test
    void transferOwnAccounts_destinationNotOwnedByUser_returnsBadRequest() throws Exception {
        // accountB1 apartine lui userB, nu lui userA
        String json = """
                {
                  "sourceAccountId": %d,
                  "destinationAccountId": %d,
                  "amount": 100,
                  "categoryId": %d,
                  "password": "%s"
                }
                """.formatted(accountA1.getAccountId(), accountB1.getAccountId(), category.getCategoryId(), PASSWORD);

        mockMvc.perform(post("/api/payments/transfer-own")
                        .param("userId", String.valueOf(userA.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        // soldurile raman neschimbate
        assertEqualsBalance(1000.0, accountRepository.findById(accountA1.getAccountId()).orElseThrow().getBalance());
        assertEqualsBalance(300.0, accountRepository.findById(accountB1.getAccountId()).orElseThrow().getBalance());
    }

    // ---------- Flux 9: schimb valutar ----------

    @Test
    void exchangeCurrency_ronToUsd_convertsUsingStoredRate() throws Exception {
        String json = """
                {
                  "sourceAccountId": %d,
                  "destinationAccountId": %d,
                  "amount": 90,
                  "categoryId": %d,
                  "description": "Schimb pentru vacanta",
                  "password": "%s"
                }
                """.formatted(accountA1.getAccountId(), accountAUsd.getAccountId(), category.getCategoryId(), PASSWORD);

        mockMvc.perform(post("/api/payments/exchange")
                        .param("userId", String.valueOf(userA.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.transactionType").value("EXCHANGE"));

        assertEqualsBalance(910.0, accountRepository.findById(accountA1.getAccountId()).orElseThrow().getBalance());
        // 90 RON / 4.5 = 20 USD
        assertEqualsBalance(20.0, accountRepository.findById(accountAUsd.getAccountId()).orElseThrow().getBalance());
    }

    @Test
    void exchangeCurrency_destinationNotOwnedByUser_returnsBadRequest() throws Exception {
        Account otherUserUsdAccount = createAccount("RO49AAAA0000000000000005", "USD", 0.0, "USD Bogdan");
        grantAccess(userB, otherUserUsdAccount, "OWNER");

        String json = """
                {
                  "sourceAccountId": %d,
                  "destinationAccountId": %d,
                  "amount": 50,
                  "categoryId": %d,
                  "password": "%s"
                }
                """.formatted(accountA1.getAccountId(), otherUserUsdAccount.getAccountId(), category.getCategoryId(), PASSWORD);

        mockMvc.perform(post("/api/payments/exchange")
                        .param("userId", String.valueOf(userA.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        assertEqualsBalance(1000.0, accountRepository.findById(accountA1.getAccountId()).orElseThrow().getBalance());
    }

    private void assertEqualsBalance(double expected, double actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual, 0.001);
    }
}
