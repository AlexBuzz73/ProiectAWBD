package com.example.demo.integration;

import com.example.demo.domain.*;
import com.example.demo.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:ownership")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ResourceAuthorizationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired AccountRepository accounts;
    @Autowired AccountAccessRepository accesses;
    @Autowired CategoryRepository categories;
    @Autowired CardRepository cards;
    @Autowired UserLimitRepository limits;
    @Autowired BankLimitRepository bankLimits;
    @Autowired TransactionRepository transactions;
    @Autowired PasswordEncoder encoder;
    private User a, b, admin;
    private Account aa, ba;
    private Category ca, cb, system;
    private Card ac, bc;
    private static final String LIMITS = """
        {"maxAmountPerTransactionRon":1000,"maxDailyAmountRon":2000,"maxDailyTransactionsCount":5}
        """;

    @BeforeEach void setup() {
        a = userEntity("a"); b = userEntity("b"); admin = userEntity("admin");
        admin.setRole("ADMIN"); users.save(admin);
        aa = account(a, "A"); ba = account(b, "B");
        ca = category(a, "Private A"); cb = category(b, "Private B");
        system = category(null, "System");
        ac = card(aa, "4111111111111111"); bc = card(ba, "4222222222222222");
        BankLimit bank = new BankLimit();
        bank.setMaxAmountPerTransactionRon(new BigDecimal("5000"));
        bank.setMaxDailyAmountRon(new BigDecimal("20000"));
        bank.setMaxDailyTransactionsCount(new BigDecimal("100"));
        bank.setStatus("ACTIVE"); bankLimits.save(bank);
    }

    private User userEntity(String name) {
        User u = new User(); u.setUsername(name); u.setEmail(name + "@ownership.test");
        u.setPasswordHash(encoder.encode("password123")); u.setRole("USER"); u.setStatus("ACTIVE");
        u.setCreatedAt(new Date()); u.setUpdatedAt(new Date()); return users.save(u);
    }
    private Account account(User owner, String suffix) {
        Account account = new Account(); account.setAlias(suffix); account.setCurrency("RON");
        account.setIban("RO49AAAA000000000000000" + suffix); account.setBalance(1000);
        account.setStatus("ACTIVE"); account.setCreatedAt(new Date()); account.setUpdatedAt(new Date());
        account.setAccountAccessList(new ArrayList<>()); accounts.save(account);
        grant(owner, account, "OWNER", "ACTIVE"); return account;
    }
    private AccountAccess grant(User u, Account account, String role, String status) {
        AccountAccess access = new AccountAccess(); access.setUser(u); access.setAccount(account);
        access.setAccessRole(role); access.setStatus(status); accesses.save(access);
        account.getAccountAccessList().add(access); return access;
    }
    private Category category(User owner, String name) {
        Category c = new Category(); c.setName(name); c.setCreatedByUser(owner);
        c.setIsSystem(owner == null ? "Y" : "N"); c.setStatus("ACTIVE"); return categories.save(c);
    }
    private Card card(Account account, String number) {
        Card c = new Card(); c.setAccount(account); c.setCardNumber(number); c.setStatus("ACTIVE");
        c.setType("DEBIT"); c.setHolderName("Test"); c.setExpirationDate(new Date()); return cards.save(c);
    }
    private String path(String template) {
        return template.replace("{A}", a.getUserId().toString()).replace("{B}", b.getUserId().toString())
                .replace("{AA}", aa.getAccountId().toString()).replace("{BA}", ba.getAccountId().toString())
                .replace("{AC}", "" + ac.getCardId()).replace("{BC}", "" + bc.getCardId())
                .replace("{CA}", "" + ca.getCategoryId()).replace("{CB}", "" + cb.getCategoryId());
    }
    private MockHttpServletRequestBuilder as(User u, String method, String path) {
        return request(HttpMethod.valueOf(method), path(path)).with(user(u.getEmail()).roles(u.getRole())).with(csrf());
    }

    @ParameterizedTest
    @CsvSource({
        "GET,/api/accounts?userId={B}", "GET,/api/accounts/paged?userId={B}",
        "GET,/api/accounts/summary/currency?userId={B}", "GET,/api/accounts/{BA}?userId={B}",
        "PUT,/api/accounts/{BA}/close?userId={B}", "GET,/api/accounts/{BA}", "PUT,/api/accounts/{BA}/close",
        "GET,/api/transactions/user?userId={B}", "GET,/api/transactions/account/{BA}",
        "GET,/api/transactions/account/{BA}?userId={B}",
        "GET,/api/users/{B}/accounts/{BA}/card", "POST,/api/users/{B}/accounts/{BA}/card",
        "GET,/api/users/me/accounts/{BA}/card", "POST,/api/users/me/accounts/{BA}/card",
        "PATCH,/api/users/me/accounts/{BA}/card/{BC}/status/BLOCKED",
        "DELETE,/api/users/me/accounts/{BA}/card/{BC}/delete",
        "PATCH,/api/users/me/accounts/{AA}/card/{BC}/status/BLOCKED",
        "DELETE,/api/users/me/accounts/{AA}/card/{BC}/delete",
        "GET,/api/users/{B}/categories", "GET,/api/users/{B}/categories/paged",
        "GET,/api/users/me/categories/{CB}", "DELETE,/api/users/me/categories/{CB}",
        "GET,/api/user/{B}/limits", "DELETE,/api/user/{B}/limits"
    })
    void foreignIdentifiersAreForbiddenAndDoNotMutateResources(String method, String endpoint) throws Exception {
        mvc.perform(as(a, method, endpoint)).andExpect(status().isForbidden());
        assertEquals(1000, ba.getBalance()); assertEquals("ACTIVE", ba.getStatus());
        assertEquals("ACTIVE", bc.getStatus()); assertEquals("Private B", cb.getName());
        assertEquals(0, transactions.count());
    }

    @ParameterizedTest
    @CsvSource({"GET,/api/admin/bank-limits", "PUT,/api/admin/bank-limits",
            "DELETE,/api/admin/bank-limits/1", "PUT,/api/admin/users/{B}/unlock",
            "POST,/api/admin/unlock-user", "POST,/api/admin/create-shared-account",
            "DELETE,/api/admin/accounts/{BA}/access"})
    void userCannotAccessAnyAdministrativeOperation(String method, String endpoint) throws Exception {
        mvc.perform(as(a, method, endpoint).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test void cannotSpoofIdentityOnCreationOrLimitsAndCategories() throws Exception {
        mvc.perform(as(a, "POST", "/api/accounts?userId={B}").contentType(MediaType.APPLICATION_JSON).content("""
            {"alias":"forged","currency":"RON","initialAmount":1,"externalIban":"RO49AAAA1B31007593840000"}
            """)).andExpect(status().isForbidden());
        mvc.perform(as(a, "PUT", "/api/user/{B}/limits").contentType(MediaType.APPLICATION_JSON).content(LIMITS))
                .andExpect(status().isForbidden());
        mvc.perform(as(a, "PUT", "/api/users/me/categories/{CB}").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"stolen\"}")).andExpect(status().isForbidden());
        mvc.perform(as(a, "POST", "/api/users/{B}/categories").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"stolen\"}")).andExpect(status().isForbidden());
        assertEquals(2, accounts.count()); assertEquals(3, categories.count()); assertEquals(0, limits.count());
    }

    private String payment(Account source, Account destination, Category category) {
        return """
            {"sourceAccountId":%d,"destinationAccountId":%d,"destinationIban":"%s",
            "amount":10,"currency":"RON","categoryId":%d,"processingType":"URGENT","password":"password123"}
            """.formatted(source.getAccountId(), destination.getAccountId(), destination.getIban(), category.getCategoryId());
    }

    @ParameterizedTest @CsvSource({"initiate", "transfer-own", "exchange"})
    void paymentIdentifiersCannotBorrowAnotherUsersAuthority(String operation) throws Exception {
        String endpoint = "/api/payments/" + operation;
        for (String body : new String[]{payment(ba, aa, ca), payment(aa, aa, cb)}) {
            mvc.perform(as(a, "POST", endpoint).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(as(a, "POST", endpoint + "?userId={B}").contentType(MediaType.APPLICATION_JSON).content(payment(ba, aa, cb)))
                .andExpect(status().isForbidden());
        if (!"initiate".equals(operation)) {
            mvc.perform(as(a, "POST", endpoint).contentType(MediaType.APPLICATION_JSON).content(payment(aa, ba, ca)))
                    .andExpect(status().isForbidden());
        }
        assertEquals(1000, aa.getBalance()); assertEquals(1000, ba.getBalance()); assertEquals(0, transactions.count());
    }

    @Test void ownResourcesAndExplicitAdminOperationsWorkWithRealLoginSession() throws Exception {
        MockHttpSession session = (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"a@ownership.test","password":"password123"}
                """)).andExpect(status().isOk()).andReturn().getRequest().getSession(false);
        assertNotNull(session);
        mvc.perform(get("/api/accounts").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].accountId").value(aa.getAccountId()));
        mvc.perform(get(path("/api/accounts?userId={B}")).session(session)).andExpect(status().isForbidden());
        mvc.perform(get(path("/api/accounts/{BA}")).session(session)).andExpect(status().isForbidden());
        mvc.perform(as(a, "GET", "/api/accounts/{AA}")).andExpect(status().isOk());
        mvc.perform(as(a, "GET", "/api/transactions/account/{AA}")).andExpect(status().isOk());
        mvc.perform(as(a, "GET", "/api/users/me/categories")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(as(a, "GET", "/api/users/me/categories/" + system.getCategoryId())).andExpect(status().isOk());
        mvc.perform(as(a, "PUT", "/api/users/me/categories/{CA}").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Updated A\"}")).andExpect(status().isOk());
        mvc.perform(as(a, "PUT", "/api/user/me/limits").contentType(MediaType.APPLICATION_JSON).content(LIMITS)).andExpect(status().isOk());
        mvc.perform(as(a, "GET", "/api/user/me/limits")).andExpect(status().isOk());
        mvc.perform(as(a, "DELETE", "/api/user/me/limits")).andExpect(status().isOk());
        mvc.perform(as(a, "PATCH", "/api/users/me/accounts/{AA}/card/{AC}/status/BLOCKED")).andExpect(status().isOk());
        mvc.perform(as(a, "DELETE", "/api/users/me/accounts/{AA}/card/{AC}/delete")).andExpect(status().isOk());
        // A may pay a third party by IBAN; that never grants access to the destination account.
        mvc.perform(as(a, "POST", "/api/payments/initiate").contentType(MediaType.APPLICATION_JSON).content(payment(aa, ba, ca)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EXECUTED"));
        assertEquals(990, aa.getBalance()); assertEquals(1010, ba.getBalance());
        mvc.perform(as(a, "GET", "/api/admin/bank-limits")).andExpect(status().isForbidden());
        mvc.perform(as(admin, "GET", "/api/admin/bank-limits")).andExpect(status().isOk());
        mvc.perform(as(admin, "PUT", "/api/admin/bank-limits").contentType(MediaType.APPLICATION_JSON).content(LIMITS)).andExpect(status().isOk());
        mvc.perform(as(admin, "POST", "/api/admin/unlock-user?email=" + b.getEmail())).andExpect(status().isOk());
        aa.setBalance(0); accounts.save(aa);
        mvc.perform(as(a, "PUT", "/api/accounts/{AA}/close")).andExpect(status().isOk());
        mvc.perform(as(a, "DELETE", "/api/users/me/categories/{CA}")).andExpect(status().isOk());
    }

    @ParameterizedTest @CsvSource({"VIEWER,ACTIVE", "OWNER,INACTIVE", "CO_OWNER,INACTIVE", "UNKNOWN,ACTIVE"})
    void insufficientOrRevokedAccessCannotMutate(String role, String state) throws Exception {
        grant(a, ba, role, state);
        mvc.perform(as(a, "PUT", "/api/accounts/{BA}/close")).andExpect(status().isForbidden());
        mvc.perform(as(a, "DELETE", "/api/users/me/accounts/{BA}/card/{BC}/delete")).andExpect(status().isForbidden());
        for (String operation : new String[]{"initiate", "transfer-own", "exchange"}) {
            mvc.perform(as(a, "POST", "/api/payments/" + operation).contentType(MediaType.APPLICATION_JSON).content(payment(ba, aa, ca)))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(as(a, "GET", "/api/accounts/{BA}"))
                .andExpect(status().is("VIEWER".equals(role) && "ACTIVE".equals(state) ? 200 : 403));
        assertEquals(1000, ba.getBalance()); assertEquals("ACTIVE", bc.getStatus());
    }

    @Test void coOwnerCanPayButCannotManageCardsOrCloseAndAdminIsNotAnOwner() throws Exception {
        grant(a, ba, "CO_OWNER", "ACTIVE");
        mvc.perform(as(a, "GET", "/api/accounts/{BA}")).andExpect(status().isOk());
        mvc.perform(as(a, "POST", "/api/payments/transfer-own").contentType(MediaType.APPLICATION_JSON).content(payment(ba, aa, ca)))
                .andExpect(status().isOk());
        mvc.perform(as(a, "POST", "/api/users/me/accounts/{BA}/card")).andExpect(status().isForbidden());
        mvc.perform(as(a, "PUT", "/api/accounts/{BA}/close")).andExpect(status().isForbidden());
        mvc.perform(as(admin, "GET", "/api/accounts/{BA}")).andExpect(status().isForbidden());
        mvc.perform(as(admin, "GET", "/api/accounts?userId={B}")).andExpect(status().isForbidden());
        mvc.perform(as(admin, "DELETE", "/api/users/me/categories/{CB}")).andExpect(status().isForbidden());
    }

    @Test void statusCodesAndSystemCategoriesRemainProtected() throws Exception {
        mvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
        mvc.perform(as(a, "GET", "/api/accounts/999999")).andExpect(status().isNotFound());
        mvc.perform(as(a, "GET", "/api/users/me/categories/999999")).andExpect(status().isNotFound());
        mvc.perform(as(a, "PATCH", "/api/users/me/accounts/{AA}/card/999999/status/BLOCKED")).andExpect(status().isNotFound());
        mvc.perform(as(a, "DELETE", "/api/users/me/categories/" + system.getCategoryId())).andExpect(status().isForbidden());
        mvc.perform(as(a, "PUT", "/api/users/me/categories/" + system.getCategoryId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"forged\"}")).andExpect(status().isForbidden());
        a.setStatus("BLOCKED"); users.save(a);
        mvc.perform(as(a, "GET", "/api/accounts")).andExpect(status().isForbidden());
        mvc.perform(get("/api/accounts").with(user("missing@ownership.test"))).andExpect(status().isForbidden());
    }

    @Test void transactionHistoryDoesNotRevealCounterpartyPrivateResources() throws Exception {
        mvc.perform(as(b, "POST", "/api/payments/initiate").contentType(MediaType.APPLICATION_JSON).content(payment(ba, aa, cb)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(cb.getCategoryId()))
                .andExpect(jsonPath("$.destinationAccountAlias").doesNotExist());
        for (String endpoint : new String[]{"/api/transactions/user", "/api/transactions/account/{AA}"}) {
            mvc.perform(as(a, "GET", endpoint)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].amount").value(10))
                    .andExpect(jsonPath("$.content[0].categoryId").doesNotExist())
                    .andExpect(jsonPath("$.content[0].categoryName").doesNotExist())
                    .andExpect(jsonPath("$.content[0].sourceAccountAlias").doesNotExist())
                    .andExpect(jsonPath("$.content[0].sourceAccountId").doesNotExist())
                    .andExpect(jsonPath("$.content[0].sourceAccountIban").value(ba.getIban()))
                    .andExpect(jsonPath("$.content[0].destinationAccountId").value(aa.getAccountId()));
        }
        mvc.perform(as(b, "GET", "/api/transactions/user")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].categoryName").value("Private B"));
    }
}
