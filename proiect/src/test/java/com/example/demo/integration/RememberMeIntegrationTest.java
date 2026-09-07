package com.example.demo.integration;

import com.example.demo.domain.Account;
import com.example.demo.domain.AccountAccess;
import com.example.demo.domain.User;
import com.example.demo.repositories.AccountAccessRepository;
import com.example.demo.repositories.AccountRepository;
import com.example.demo.repositories.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:remembermetest")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RememberMeIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private AccountAccessRepository accountAccessRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User testUser;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("rememberUser");
        testUser.setEmail("remember@test.com");
        testUser.setPasswordHash(passwordEncoder.encode("Password123!"));
        testUser.setRole("USER");
        testUser.setStatus("ACTIVE");
        testUser.setCreatedAt(new Date());
        testUser.setUpdatedAt(new Date());
        testUser = userRepository.save(testUser);

        testAccount = new Account();
        testAccount.setAlias("Remember Account");
        testAccount.setCurrency("RON");
        testAccount.setIban("RO49AAAA9999000000000001");
        testAccount.setBalance(500.0);
        testAccount.setStatus("ACTIVE");
        testAccount.setCreatedAt(new Date());
        testAccount.setUpdatedAt(new Date());
        testAccount.setAccountAccessList(new ArrayList<>());
        testAccount = accountRepository.save(testAccount);

        AccountAccess access = new AccountAccess();
        access.setUser(testUser);
        access.setAccount(testAccount);
        access.setAccessRole("OWNER");
        access.setStatus("ACTIVE");
        accountAccessRepository.save(access);
    }

    @Test
    void loginWithRememberMeTrue_setsRememberMeCookie() throws Exception {
        String loginJson = """
                {
                    "email": "remember@test.com",
                    "password": "Password123!",
                    "rememberMe": true
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        Cookie rememberCookie = response.getCookie("remember-me");
        assertNotNull(rememberCookie, "remember-me cookie must be set when rememberMe is true");
        assertTrue(rememberCookie.getValue().length() > 10);
        assertTrue(rememberCookie.getMaxAge() > 0, "Cookie max age should be positive");
    }

    @Test
    void loginWithRememberMeFalse_doesNotSetRememberMeCookie() throws Exception {
        String loginJson = """
                {
                    "email": "remember@test.com",
                    "password": "Password123!",
                    "rememberMe": false
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        Cookie rememberCookie = response.getCookie("remember-me");
        assertNull(rememberCookie, "remember-me cookie must not be set when rememberMe is false");
    }

    @Test
    void accessingProtectedResourceWithOnlyRememberMeCookie_authenticatesUserSuccessfully() throws Exception {
        // 1. Log in with rememberMe = true to acquire the cookie
        String loginJson = """
                {
                    "email": "remember@test.com",
                    "password": "Password123!",
                    "rememberMe": true
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        Cookie rememberCookie = loginResult.getResponse().getCookie("remember-me");
        assertNotNull(rememberCookie);

        // 2. Perform a GET /api/accounts WITHOUT any session, supplying ONLY the remember-me cookie
        mockMvc.perform(get("/api/accounts")
                        .cookie(rememberCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].alias").value("Remember Account"))
                .andExpect(jsonPath("$[0].currency").value("RON"));
    }

    @Test
    void logoutClearsRememberMeCookie() throws Exception {
        // Log in with rememberMe
        String loginJson = """
                {
                    "email": "remember@test.com",
                    "password": "Password123!",
                    "rememberMe": true
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        Cookie rememberCookie = loginResult.getResponse().getCookie("remember-me");
        assertNotNull(rememberCookie);

        // Perform logout with remember-me cookie and csrf
        MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .cookie(rememberCookie))
                .andExpect(status().isOk())
                .andReturn();

        // The logout handler must delete the cookie (maxAge = 0)
        Cookie clearedCookie = logoutResult.getResponse().getCookie("remember-me");
        assertNotNull(clearedCookie);
        assertEquals(0, clearedCookie.getMaxAge(), "Cookie should be cleared with Max-Age 0");
    }

    @Test
    void invalidRememberMeCookie_isRejected() throws Exception {
        Cookie invalidCookie = new Cookie("remember-me", "invalid-token-content-that-does-not-decode");

        // Unauthenticated access without valid credentials must return 401
        mockMvc.perform(get("/api/accounts")
                        .cookie(invalidCookie))
                .andExpect(status().isUnauthorized());
    }
}
