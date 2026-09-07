package com.example.demo.integration;

import com.example.demo.domain.Account;
import com.example.demo.domain.AccountAccess;
import com.example.demo.domain.User;
import com.example.demo.repositories.AccountAccessRepository;
import com.example.demo.repositories.AccountRepository;
import com.example.demo.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:validationerrortest")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ValidationAndErrorHandlingIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private AccountAccessRepository accountAccessRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User userA;
    private User adminUser;
    private Account accountA;

    @BeforeEach
    void setUp() {
        userA = new User();
        userA.setUsername("userA");
        userA.setEmail("usera@validation.test");
        userA.setPasswordHash(passwordEncoder.encode("Password123!"));
        userA.setRole("USER");
        userA.setStatus("ACTIVE");
        userA.setCreatedAt(new Date());
        userA.setUpdatedAt(new Date());
        userA = userRepository.save(userA);

        adminUser = new User();
        adminUser.setUsername("adminUser");
        adminUser.setEmail("admin@validation.test");
        adminUser.setPasswordHash(passwordEncoder.encode("Password123!"));
        adminUser.setRole("ADMIN");
        adminUser.setStatus("ACTIVE");
        adminUser.setCreatedAt(new Date());
        adminUser.setUpdatedAt(new Date());
        adminUser = userRepository.save(adminUser);

        accountA = new Account();
        accountA.setAlias("Cont Valid");
        accountA.setCurrency("RON");
        accountA.setIban("RO49AAAA8888000000000001");
        accountA.setBalance(1500.0);
        accountA.setStatus("ACTIVE");
        accountA.setCreatedAt(new Date());
        accountA.setUpdatedAt(new Date());
        accountA.setAccountAccessList(new ArrayList<>());
        accountA = accountRepository.save(accountA);

        AccountAccess access = new AccountAccess();
        access.setUser(userA);
        access.setAccount(accountA);
        access.setAccessRole("OWNER");
        access.setStatus("ACTIVE");
        accountAccessRepository.save(access);
    }

    // ================= 404 NOT FOUND TESTS =================

    @Test
    void authenticatedRequestToNonExistentEndpoint_returns404() throws Exception {
        // Solves the previously documented issue: GET authenticated /api/does-not-exist returned 500
        mockMvc.perform(get("/api/does-not-exist")
                        .with(user(userA.getEmail()).roles(userA.getRole())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resursa nu a fost gasita"));
    }

    @Test
    void unauthenticatedRequestToNonExistentEndpoint_returns401() throws Exception {
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Autentificare necesara"));
    }

    @Test
    void nonExistentAccount_returns404() throws Exception {
        mockMvc.perform(get("/api/accounts/999999")
                        .with(user(userA.getEmail()).roles(userA.getRole())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resursa nu a fost gasita"));
    }

    @Test
    void nonExistentCategory_returns404() throws Exception {
        mockMvc.perform(get("/api/users/me/categories/999999")
                        .with(user(userA.getEmail()).roles(userA.getRole())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resursa nu a fost gasita"));
    }

    // ================= 400 BAD REQUEST & VALIDATION TESTS =================

    @Test
    void categoryCreation_blankName_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/users/me/categories")
                        .with(user(userA.getEmail()).roles(userA.getRole()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Category name is required!"));
    }

    @Test
    void sharedAccountCreation_blankAlias_returns400() throws Exception {
        String invalidJson = """
                {
                    "alias": "",
                    "currency": "RON",
                    "users": [
                        {"email": "usera@validation.test", "role": "OWNER"}
                    ]
                }
                """;

        mockMvc.perform(post("/api/admin/create-shared-account")
                        .with(user(adminUser.getEmail()).roles(adminUser.getRole()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.alias").value("Aliasul contului este obligatoriu."));
    }

    @Test
    void sharedAccountCreation_invalidCurrency_returns400() throws Exception {
        String invalidJson = """
                {
                    "alias": "Cont Comun",
                    "currency": "GBP",
                    "users": [
                        {"email": "usera@validation.test", "role": "OWNER"}
                    ]
                }
                """;

        mockMvc.perform(post("/api/admin/create-shared-account")
                        .with(user(adminUser.getEmail()).roles(adminUser.getRole()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.currency").value("Valuta trebuie să fie RON, EUR sau USD."));
    }

    @Test
    void sharedAccountCreation_emptyUsersList_returns400() throws Exception {
        String invalidJson = """
                {
                    "alias": "Cont Comun",
                    "currency": "RON",
                    "users": []
                }
                """;

        mockMvc.perform(post("/api/admin/create-shared-account")
                        .with(user(adminUser.getEmail()).roles(adminUser.getRole()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userLimits_nullOrNegativeValues_returns400() throws Exception {
        String invalidJson = """
                {
                    "maxAmountPerTransactionRon": -50,
                    "maxDailyAmountRon": 1000,
                    "maxDailyTransactionsCount": 5
                }
                """;

        mockMvc.perform(put("/api/user/me/limits")
                        .with(user(userA.getEmail()).roles(userA.getRole()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonBody_returns400() throws Exception {
        mockMvc.perform(post("/api/users/me/categories")
                        .with(user(userA.getEmail()).roles(userA.getRole()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("NOT_VALID_JSON{:::"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Format JSON invalid sau corp cerere lipsa"));
    }

    @Test
    void methodArgumentTypeMismatch_returns400() throws Exception {
        // Passing a non-numeric string to an integer path variable
        mockMvc.perform(get("/api/users/me/categories/not-a-number")
                        .with(user(userA.getEmail()).roles(userA.getRole())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Parametru invalid: categoryId"));
    }

    // ================= 405 METHOD NOT ALLOWED TESTS =================

    @Test
    void unsupportedHttpMethod_returns405() throws Exception {
        mockMvc.perform(post("/api/accounts/summary/currency")
                        .with(user(userA.getEmail()).roles(userA.getRole()))
                        .with(csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("Metoda HTTP nu este suportata"));
    }

    // ================= 403 FORBIDDEN TESTS =================

    @Test
    void regularUserAccessingAdminRoute_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/bank-limits")
                        .with(user(userA.getEmail()).roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acces interzis"));
    }
}
