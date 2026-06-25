package com.example.demo.integration;

import com.example.demo.domain.Category;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CategoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

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
    private UserLimitRepository userLimitRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private IndividualRepository individualRepository;

    private User activeUser;
    private Category systemCategory;
    private Category userCategory;


    @BeforeEach
    void setUp() {
        scheduledPaymentRepository.deleteAllInBatch();
        transactionRepository.deleteAllInBatch();
        cardRepository.deleteAllInBatch();
        accountAccessRepository.deleteAllInBatch();
        userLimitRepository.deleteAllInBatch();
        categoryRepository.deleteAll();
        exchangeRateRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
        userRepository.deleteAll();
        individualRepository.deleteAllInBatch();

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

        systemCategory = new Category();
        systemCategory.setName("Food");
        systemCategory.setIsSystem("Y");
        systemCategory.setStatus("ACTIVE");
        systemCategory.setCreatedAt(new Date());
        systemCategory.setUpdatedAt(new Date());
        systemCategory = categoryRepository.save(systemCategory);

        userCategory = new Category();
        userCategory.setName("Personal");
        userCategory.setIsSystem("N");
        userCategory.setCreatedByUser(activeUser);
        userCategory.setStatus("ACTIVE");
        userCategory.setCreatedAt(new Date());
        userCategory.setUpdatedAt(new Date());
        userCategory = categoryRepository.save(userCategory);
    }

    @Test
    void getAvailableCategories_shouldReturnSystemAndUserCategories() throws Exception {
        mockMvc.perform(get("/api/users/" + activeUser.getUserId() + "/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void createCategory_shouldCreateNewUserCategory() throws Exception {
        String jsonBody = """
                {
                  "name": "Travel"
                }
                """;

        mockMvc.perform(
                        post("/api/users/" + activeUser.getUserId() + "/categories")
                                .contentType("application/json")
                                .content(jsonBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Travel")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    void createCategory_shouldReturnBadRequest_whenCategoryAlreadyExists() throws Exception {
        String jsonBody = """
                {
                  "name": "Food"
                }
                """;

        mockMvc.perform(
                        post("/api/users/" + activeUser.getUserId() + "/categories")
                                .contentType("application/json")
                                .content(jsonBody)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteCategory_shouldSetCategoryInactive() throws Exception {
        mockMvc.perform(delete("/api/users/" + activeUser.getUserId() + "/categories/" + userCategory.getCategoryId()))
                .andExpect(status().isOk());

        Category deletedCategory = categoryRepository.findById(userCategory.getCategoryId())
                .orElseThrow();

        org.junit.jupiter.api.Assertions.assertEquals("INACTIVE", deletedCategory.getStatus());
    }

    @Test
    void deleteCategory_shouldReturnBadRequest_whenCategoryIsSystemCategory() throws Exception {
        mockMvc.perform(delete("/api/users/" + activeUser.getUserId() + "/categories/" + systemCategory.getCategoryId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCategory_shouldUpdateCategoryName() throws Exception {
        String jsonBody = """
                {
                  "name": "Updated Personal"
                }
                """;

        mockMvc.perform(
                        put("/api/users/" + activeUser.getUserId() + "/categories/" + userCategory.getCategoryId())
                                .contentType("application/json")
                                .content(jsonBody)
                )
                .andExpect(status().isOk());

        Category updatedCategory = categoryRepository.findById(userCategory.getCategoryId())
                .orElseThrow();

        org.junit.jupiter.api.Assertions.assertEquals("Updated Personal", updatedCategory.getName());
    }

    @Test
    void getCategory_shouldReturnCategory() throws Exception {
        mockMvc.perform(get("/api/users/" + activeUser.getUserId() + "/categories/" + userCategory.getCategoryId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Personal")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }
}