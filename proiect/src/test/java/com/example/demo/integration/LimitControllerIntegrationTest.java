package com.example.demo.integration;

import com.example.demo.domain.BankLimit;
import com.example.demo.domain.User;
import com.example.demo.repositories.BankLimitRepository;
import com.example.demo.repositories.UserLimitRepository;
import com.example.demo.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class LimitControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BankLimitRepository bankLimitRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserLimitRepository userLimitRepository;

    private User activeUser;

    @BeforeEach
    void setUp() {
        userLimitRepository.deleteAll();
        userRepository.deleteAll();
        bankLimitRepository.deleteAll();

        BankLimit bankLimit = new BankLimit();
        bankLimit.setMaxAmountPerTransactionRon(new BigDecimal("5000"));
        bankLimit.setMaxDailyAmountRon(new BigDecimal("20000"));
        bankLimit.setMaxDailyTransactionsCount(new BigDecimal("10"));
        bankLimit.setStatus("ACTIVE");
        bankLimit.setCreatedAt(new Date());
        bankLimit.setUpdatedAt(new Date());
        bankLimitRepository.save(bankLimit);

        activeUser = new User();
        activeUser.setUsername("testuser");
        activeUser.setEmail("testuser@gmail.com");
        activeUser.setPasswordHash("encoded-password");
        activeUser.setRole("USER");
        activeUser.setFailedLoginAttempts(0);
        activeUser.setStatus("ACTIVE");
        activeUser.setCreatedAt(new Date());
        activeUser.setUpdatedAt(new Date());
        activeUser = userRepository.save(activeUser);
    }

    @Test
    void getBankLimits_shouldReturnActiveBankLimits() throws Exception {
        mockMvc.perform(get("/api/admin/bank-limits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxAmountPerTransactionRon", is(5000.0)))
                .andExpect(jsonPath("$.maxDailyAmountRon", is(20000.0)))
                .andExpect(jsonPath("$.maxDailyTransactionsCount", is(10.0)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    void updateBankLimits_shouldUpdateActiveBankLimits() throws Exception {
        String jsonBody = """
                {
                  "maxAmountPerTransactionRon": 7000,
                  "maxDailyAmountRon": 25000,
                  "maxDailyTransactionsCount": 15
                }
                """;

        mockMvc.perform(
                        put("/api/admin/bank-limits")
                                .contentType("application/json")
                                .content(jsonBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxAmountPerTransactionRon", is(7000)))
                .andExpect(jsonPath("$.maxDailyAmountRon", is(25000)))
                .andExpect(jsonPath("$.maxDailyTransactionsCount", is(15)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    void updateUserLimits_shouldCreateUserLimitsForActiveUser() throws Exception {
        String jsonBody = """
                {
                  "maxAmountPerTransactionRon": 3000,
                  "maxDailyAmountRon": 10000,
                  "maxDailyTransactionsCount": 5
                }
                """;

        mockMvc.perform(
                        put("/api/user/" + activeUser.getUserId() + "/limits")
                                .contentType("application/json")
                                .content(jsonBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxAmountPerTransactionRon", is(3000)))
                .andExpect(jsonPath("$.maxDailyAmountRon", is(10000)))
                .andExpect(jsonPath("$.maxDailyTransactionsCount", is(5)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    void updateUserLimits_shouldReturnBadRequest_whenValuesAreInvalid() throws Exception {
        String jsonBody = """
                {
                  "maxAmountPerTransactionRon": -100,
                  "maxDailyAmountRon": 10000,
                  "maxDailyTransactionsCount": 5
                }
                """;

        mockMvc.perform(
                        put("/api/user/" + activeUser.getUserId() + "/limits")
                                .contentType("application/json")
                                .content(jsonBody)
                )
                .andExpect(status().isBadRequest());
    }
}