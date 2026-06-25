package com.example.demo.integration;

import com.example.demo.domain.User;
import com.example.demo.repositories.AccountAccessRepository;
import com.example.demo.repositories.AccountRepository;
import com.example.demo.repositories.CardRepository;
import com.example.demo.repositories.CategoryRepository;
import com.example.demo.repositories.ExchangeRateRepository;
import com.example.demo.repositories.IndividualRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IndividualRepository individualRepository;

    @Autowired
    private AccountAccessRepository accountAccessRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private ScheduledPaymentRepository scheduledPaymentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserLimitRepository userLimitRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;


    @BeforeEach
    void cleanDatabase() {
        scheduledPaymentRepository.deleteAllInBatch();
        transactionRepository.deleteAllInBatch();
        cardRepository.deleteAllInBatch();
        accountAccessRepository.deleteAllInBatch();
        userLimitRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        exchangeRateRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        individualRepository.deleteAllInBatch();
    }

    @Test
    void shouldRegisterAndLoginSuccessfully() throws Exception {
        String registerJson = """
                {
                  "individual": {
                    "firstName": "Alex",
                    "lastName": "Buzatu",
                    "cnp": "1234567890123",
                    "phoneNumber": "0712345678",
                    "dateOfBirth": "2003-01-01"
                  },
                  "user": {
                    "username": "alex123",
                    "email": "alex@test.com",
                    "password": "password123"
                  }
                }
                """;

        mockMvc.perform(post("/api/auth/register")
               .contentType(MediaType.APPLICATION_JSON)
               .content(registerJson))
               .andExpect(status().isOk());

        assertTrue(userRepository.existsByEmail("alex@test.com"));
        assertTrue(individualRepository.existsByCnp("1234567890123"));

        User savedUser = userRepository.findByEmail("alex@test.com").orElseThrow();

        assertNotEquals("password123", savedUser.getPasswordHash());
        assertEquals("USER", savedUser.getRole());
        assertEquals("ACTIVE", savedUser.getStatus());
        assertEquals(0, savedUser.getFailedLoginAttempts());

        String loginJson = """
                {
                  "email": "alex@test.com",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
               .contentType(MediaType.APPLICATION_JSON)
               .content(loginJson))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.userId").value(savedUser.getUserId()))
               .andExpect(jsonPath("$.username").value("alex123"))
               .andExpect(jsonPath("$.email").value("alex@test.com"))
               .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void shouldBlockUserAfterThreeFailedLoginsThenUnlockAndLoginSuccessfully() throws Exception {
        String registerJson = """
                {
                  "individual": {
                    "firstName": "Alex",
                    "lastName": "Buzatu",
                    "cnp": "1234567890123",
                    "phoneNumber": "0722222222",
                    "dateOfBirth": "1998-05-10"
                  },
                  "user": {
                    "username": "alex123",
                    "email": "alex@test.com",
                    "password": "password123"
                  }
                }
                """;

        mockMvc.perform(post("/api/auth/register")
               .contentType(MediaType.APPLICATION_JSON)
               .content(registerJson))
               .andExpect(status().isOk());

        String wrongLoginJson = """
                {
                  "email": "alex@test.com",
                  "password": "wrongPass"
                }
                """;

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(wrongLoginJson))
                   .andExpect(status().isBadRequest())
                   .andExpect(content().string("Invalid email or password!"));
        }

        User blockedUser = userRepository.findByEmail("alex@test.com").orElseThrow();

        assertEquals(3, blockedUser.getFailedLoginAttempts());
        assertEquals("BLOCKED", blockedUser.getStatus());

        mockMvc.perform(put("/api/admin/users/{userId}/unlock", blockedUser.getUserId()))
               .andExpect(status().isOk());

        User unlockedUser = userRepository.findByEmail("alex@test.com").orElseThrow();

        assertEquals(0, unlockedUser.getFailedLoginAttempts());
        assertEquals("ACTIVE", unlockedUser.getStatus());

        String correctLoginJson = """
                {
                  "email": "alex@test.com",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
               .contentType(MediaType.APPLICATION_JSON)
               .content(correctLoginJson))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.email").value("alex@test.com"))
               .andExpect(jsonPath("$.username").value("alex123"))
               .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void shouldReturnBadRequestWhenRegisterDataIsInvalid() throws Exception {
        String invalidRegisterJson = """
                {
                  "individual": {
                    "lastName": "Popescu",
                    "cnp": "3234567890123",
                    "phoneNumber": "0733333333",
                    "dateOfBirth": "2000-01-01"
                  },
                  "user": {
                    "username": "invalidUser",
                    "email": "invalid@test.com",
                    "password": "password123"
                  }
                }
                """;

        mockMvc.perform(post("/api/auth/register")
               .contentType(MediaType.APPLICATION_JSON)
               .content(invalidRegisterJson))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.['individual.firstName']").value("First name is required!"));
    }
}