package com.example.transactionservice;

import com.example.transactionservice.client.AccountClient;
import com.example.transactionservice.domain.*;
import com.example.transactionservice.dto.*;
import com.example.transactionservice.repositories.*;
import com.example.transactionservice.services.ExchangeRateUpdateJob;
import com.example.transactionservice.services.PaymentJob;
import com.example.transactionservice.services.ScheduledPaymentJob;
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

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwtConfig.class)
@ActiveProfiles("test")
public class TransactionServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TestTokenGenerator tokenGenerator;

    @MockitoBean
    private AccountClient accountClient;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ScheduledPaymentRepository scheduledPaymentRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private PaymentJob paymentJob;

    @Autowired
    private ScheduledPaymentJob scheduledPaymentJob;

    @Autowired
    private ExchangeRateUpdateJob exchangeRateUpdateJob;

    private String user1Token;
    private String user2Token;
    private String adminToken;

    private Category systemCategory;
    private Category user1Category;
    private Category user2Category;
    private ExchangeRate eurRate;
    private ExchangeRate usdRate;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        scheduledPaymentRepository.deleteAll();
        categoryRepository.deleteAll();
        tagRepository.deleteAll();
        exchangeRateRepository.deleteAll();

        user1Token = tokenGenerator.generateToken(1, "user1", "user1@bank.com", "USER");
        user2Token = tokenGenerator.generateToken(2, "user2", "user2@bank.com", "USER");
        adminToken = tokenGenerator.generateToken(99, "admin", "admin@bank.com", "ADMIN");

        // Seed categories
        systemCategory = new Category();
        systemCategory.setName("Utilitati");
        systemCategory.setIsSystem("Y");
        systemCategory.setStatus("ACTIVE");
        systemCategory.setCreatedAt(new Date());
        systemCategory = categoryRepository.save(systemCategory);

        user1Category = new Category();
        user1Category.setName("Personale User1");
        user1Category.setIsSystem("N");
        user1Category.setCreatedByUserId(1);
        user1Category.setStatus("ACTIVE");
        user1Category.setCreatedAt(new Date());
        user1Category = categoryRepository.save(user1Category);

        user2Category = new Category();
        user2Category.setName("Secret User2");
        user2Category.setIsSystem("N");
        user2Category.setCreatedByUserId(2);
        user2Category.setStatus("ACTIVE");
        user2Category.setCreatedAt(new Date());
        user2Category = categoryRepository.save(user2Category);

        // Seed Exchange Rates
        Date today = new Date();
        eurRate = new ExchangeRate();
        eurRate.setCurrencyFrom("EUR");
        eurRate.setCurrencyTo("RON");
        eurRate.setRate(new BigDecimal("4.975000"));
        eurRate.setRateDate(today);
        eurRate.setSource("BNR");
        eurRate.setCreatedAt(today);
        exchangeRateRepository.save(eurRate);

        usdRate = new ExchangeRate();
        usdRate.setCurrencyFrom("USD");
        usdRate.setCurrencyTo("RON");
        usdRate.setRate(new BigDecimal("4.550000"));
        usdRate.setRateDate(today);
        usdRate.setSource("BNR");
        usdRate.setCreatedAt(today);
        exchangeRateRepository.save(usdRate);

        // Default mock behaviors for AccountClient
        Mockito.when(accountClient.getUserLimits(anyInt())).thenReturn(
                new UserLimitResponseDTO(1, new BigDecimal("10000.00"), new BigDecimal("50000.00"), new BigDecimal("10"), "ACTIVE")
        );
    }

    // ==========================================
    // 1. PAYMENT INITIATION TESTS
    // ==========================================

    @Test
    @DisplayName("1. Initiate standard payment -> status PENDING_EXECUTION")
    void testInitiatePaymentStandardSuccess() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont Principal", "RON", new BigDecimal("5000.00"), "ACTIVE");
        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.checkAccess(1L, 1, "CO_OWNER")).thenReturn(true);
        Mockito.when(accountClient.checkAccess(1L, 1, "VIEWER")).thenReturn(true);
        Mockito.when(accountClient.getAccountByIban("RO99EXTR0000000000000099")).thenReturn(null);

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setSourceAccountId(1L);
        request.setDestinationIban("RO99EXTR0000000000000099");
        request.setAmount(new BigDecimal("150.00"));
        request.setCurrency("RON");
        request.setCategoryId(systemCategory.getCategoryId());
        request.setProcessingType("STANDARD");
        request.setDescription("Factura curent");

        mockMvc.perform(post("/api/payments/initiate")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_EXECUTION"))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.currency").value("RON"))
                .andExpect(jsonPath("$.transactionType").value("EXTERNAL"));
    }

    @Test
    @DisplayName("2. Initiate urgent payment -> status EXECUTED directly with debit")
    void testInitiatePaymentUrgentSuccess() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont Principal", "RON", new BigDecimal("5000.00"), "ACTIVE");
        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.checkAccess(1L, 1, "CO_OWNER")).thenReturn(true);
        Mockito.when(accountClient.getAccountByIban("RO99EXTR0000000000000099")).thenReturn(null);
        Mockito.when(accountClient.debit(eq(1L), any(BigDecimal.class), anyString())).thenReturn(source);

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setSourceAccountId(1L);
        request.setDestinationIban("RO99EXTR0000000000000099");
        request.setAmount(new BigDecimal("200.00"));
        request.setCurrency("RON");
        request.setCategoryId(systemCategory.getCategoryId());
        request.setProcessingType("URGENT");
        request.setDescription("Plata urgenta chirie");

        mockMvc.perform(post("/api/payments/initiate")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.amount").value(200.00));

        Mockito.verify(accountClient, Mockito.times(1)).debit(eq(1L), eq(new BigDecimal("200.00")), anyString());
    }

    @Test
    @DisplayName("3. Initiate scheduled payment -> status PENDING_EXECUTION and ScheduledPayment ACTIVE")
    void testInitiatePaymentScheduledSuccess() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont Principal", "RON", new BigDecimal("5000.00"), "ACTIVE");
        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.checkAccess(1L, 1, "CO_OWNER")).thenReturn(true);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 5);
        Date futureDate = cal.getTime();

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setSourceAccountId(1L);
        request.setDestinationIban("RO99EXTR0000000000000099");
        request.setAmount(new BigDecimal("300.00"));
        request.setCurrency("RON");
        request.setCategoryId(systemCategory.getCategoryId());
        request.setProcessingType("PROGRAMAT");
        request.setScheduledDate(futureDate);
        request.setDescription("Plata programata intretinere");

        mockMvc.perform(post("/api/payments/initiate")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_EXECUTION"));

        List<ScheduledPayment> scheduledList = scheduledPaymentRepository.findAll();
        assertThat(scheduledList).hasSize(1);
        assertThat(scheduledList.get(0).getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("4. Initiate payment to internal IBAN -> transactionType INTERNAL")
    void testInitiatePaymentInternalSuccess() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont Sursa", "RON", new BigDecimal("5000.00"), "ACTIVE");
        AccountInternalSummaryDTO dest = new AccountInternalSummaryDTO(2L, "RO11BANK0000000000000002", "Cont Destinatie", "RON", new BigDecimal("100.00"), "ACTIVE");

        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.checkAccess(1L, 1, "CO_OWNER")).thenReturn(true);
        Mockito.when(accountClient.getAccountByIban("RO11BANK0000000000000002")).thenReturn(dest);

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setSourceAccountId(1L);
        request.setDestinationIban("RO11BANK0000000000000002");
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("RON");
        request.setCategoryId(systemCategory.getCategoryId());
        request.setProcessingType("STANDARD");
        request.setDescription("Transfer catre coleg");

        mockMvc.perform(post("/api/payments/initiate")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("INTERNAL"))
                .andExpect(jsonPath("$.destinationAccountId").value(2L))
                .andExpect(jsonPath("$.destinationAccountIban").value("RO11BANK0000000000000002"));
    }

    @Test
    @DisplayName("5. Initiate payment to external IBAN -> transactionType EXTERNAL")
    void testInitiatePaymentExternalSuccess() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont Sursa", "RON", new BigDecimal("5000.00"), "ACTIVE");

        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.checkAccess(1L, 1, "CO_OWNER")).thenReturn(true);
        Mockito.when(accountClient.getAccountByIban("RO44INGB0000000000000044")).thenReturn(null);

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setSourceAccountId(1L);
        request.setDestinationIban("RO44INGB0000000000000044");
        request.setAmount(new BigDecimal("50.00"));
        request.setCurrency("RON");
        request.setCategoryId(systemCategory.getCategoryId());
        request.setProcessingType("STANDARD");
        request.setDescription("Plata externa");

        mockMvc.perform(post("/api/payments/initiate")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("EXTERNAL"))
                .andExpect(jsonPath("$.destinationIban").value("RO44INGB0000000000000044"));
    }

    @Test
    @DisplayName("6. Payment failure: insufficient funds (400)")
    void testInitiatePaymentFailureInsufficientFunds() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont Sursa", "RON", new BigDecimal("50.00"), "ACTIVE");

        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.checkAccess(1L, 1, "CO_OWNER")).thenReturn(true);

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setSourceAccountId(1L);
        request.setDestinationIban("RO99EXTR0000000000000099");
        request.setAmount(new BigDecimal("500.00"));
        request.setCurrency("RON");
        request.setCategoryId(systemCategory.getCategoryId());
        request.setProcessingType("STANDARD");

        mockMvc.perform(post("/api/payments/initiate")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Fonduri insuficiente")));
    }

    @Test
    @DisplayName("7. Payment failure: limit exceeded (400)")
    void testInitiatePaymentFailureLimitExceeded() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont Sursa", "RON", new BigDecimal("50000.00"), "ACTIVE");
        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.checkAccess(1L, 1, "CO_OWNER")).thenReturn(true);
        Mockito.when(accountClient.getUserLimits(1)).thenReturn(
                new UserLimitResponseDTO(1, new BigDecimal("1000.00"), new BigDecimal("5000.00"), new BigDecimal("5"), "ACTIVE")
        );

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setSourceAccountId(1L);
        request.setDestinationIban("RO99EXTR0000000000000099");
        request.setAmount(new BigDecimal("2000.00"));
        request.setCurrency("RON");
        request.setCategoryId(systemCategory.getCategoryId());
        request.setProcessingType("STANDARD");

        mockMvc.perform(post("/api/payments/initiate")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Limita depasita")));
    }

    @Test
    @DisplayName("8. Payment failure: inactive source account (400)")
    void testInitiatePaymentFailureInactiveAccount() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont Sursa", "RON", new BigDecimal("5000.00"), "INACTIVE");
        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.checkAccess(1L, 1, "CO_OWNER")).thenReturn(true);

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setSourceAccountId(1L);
        request.setDestinationIban("RO99EXTR0000000000000099");
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("RON");
        request.setCategoryId(systemCategory.getCategoryId());
        request.setProcessingType("STANDARD");

        mockMvc.perform(post("/api/payments/initiate")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("activ")));
    }

    @Test
    @DisplayName("9. Payment failure: currency mismatch (400)")
    void testInitiatePaymentFailureCurrencyMismatch() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont Sursa", "EUR", new BigDecimal("5000.00"), "ACTIVE");
        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.checkAccess(1L, 1, "CO_OWNER")).thenReturn(true);

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setSourceAccountId(1L);
        request.setDestinationIban("RO99EXTR0000000000000099");
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("RON"); // Trying to pay in RON from EUR account
        request.setCategoryId(systemCategory.getCategoryId());
        request.setProcessingType("STANDARD");

        mockMvc.perform(post("/api/payments/initiate")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Valuta")));
    }

    @Test
    @DisplayName("10. Payment failure: user has no access to account (403)")
    void testInitiatePaymentFailureNoAccess() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont Sursa", "RON", new BigDecimal("5000.00"), "ACTIVE");
        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.checkAccess(1L, 1, "CO_OWNER")).thenReturn(false);

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setSourceAccountId(1L);
        request.setDestinationIban("RO99EXTR0000000000000099");
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("RON");
        request.setCategoryId(systemCategory.getCategoryId());
        request.setProcessingType("STANDARD");

        mockMvc.perform(post("/api/payments/initiate")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("11. Payment failure: VIEWER role cannot initiate payment (403)")
    void testInitiatePaymentFailureViewerRole() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont Sursa", "RON", new BigDecimal("5000.00"), "ACTIVE");
        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.checkAccess(1L, 1, "CO_OWNER")).thenReturn(false); // VIEWER is not allowed CO_OWNER (PAY)

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setSourceAccountId(1L);
        request.setDestinationIban("RO99EXTR0000000000000099");
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("RON");
        request.setCategoryId(systemCategory.getCategoryId());
        request.setProcessingType("STANDARD");

        mockMvc.perform(post("/api/payments/initiate")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("12. Payment failure: category not found (404)")
    void testInitiatePaymentFailureCategoryNotFound() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont Sursa", "RON", new BigDecimal("5000.00"), "ACTIVE");
        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.checkAccess(1L, 1, "CO_OWNER")).thenReturn(true);

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setSourceAccountId(1L);
        request.setDestinationIban("RO99EXTR0000000000000099");
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("RON");
        request.setCategoryId(99999);
        request.setProcessingType("STANDARD");

        mockMvc.perform(post("/api/payments/initiate")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("13. Payment failure: inactive category (404)")
    void testInitiatePaymentFailureInactiveCategory() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont Sursa", "RON", new BigDecimal("5000.00"), "ACTIVE");
        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.checkAccess(1L, 1, "CO_OWNER")).thenReturn(true);

        user1Category.setStatus("INACTIVE");
        categoryRepository.save(user1Category);

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setSourceAccountId(1L);
        request.setDestinationIban("RO99EXTR0000000000000099");
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("RON");
        request.setCategoryId(user1Category.getCategoryId());
        request.setProcessingType("STANDARD");

        mockMvc.perform(post("/api/payments/initiate")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("14. Payment failure: other user private category (403)")
    void testInitiatePaymentFailureOtherUserCategory() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont Sursa", "RON", new BigDecimal("5000.00"), "ACTIVE");
        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.checkAccess(1L, 1, "CO_OWNER")).thenReturn(true);

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setSourceAccountId(1L);
        request.setDestinationIban("RO99EXTR0000000000000099");
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("RON");
        request.setCategoryId(user2Category.getCategoryId()); // belongs to user 2
        request.setProcessingType("STANDARD");

        mockMvc.perform(post("/api/payments/initiate")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("15. Payment failure: scheduled date in the past (400)")
    void testInitiatePaymentFailurePastDate() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont Sursa", "RON", new BigDecimal("5000.00"), "ACTIVE");
        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.checkAccess(1L, 1, "CO_OWNER")).thenReturn(true);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -2);
        Date pastDate = cal.getTime();

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setSourceAccountId(1L);
        request.setDestinationIban("RO99EXTR0000000000000099");
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("RON");
        request.setCategoryId(systemCategory.getCategoryId());
        request.setProcessingType("PROGRAMAT");
        request.setScheduledDate(pastDate);

        mockMvc.perform(post("/api/payments/initiate")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("viitor")));
    }

    @Test
    @DisplayName("16. Payment failure: invalid IBAN format (400)")
    void testInitiatePaymentFailureInvalidIban() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setSourceAccountId(1L);
        request.setDestinationIban("INVALID-IBAN");
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("RON");
        request.setCategoryId(systemCategory.getCategoryId());
        request.setProcessingType("STANDARD");

        mockMvc.perform(post("/api/payments/initiate")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("17. Payment failure: negative or zero amount (400)")
    void testInitiatePaymentFailureNegativeAmount() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setSourceAccountId(1L);
        request.setDestinationIban("RO99EXTR0000000000000099");
        request.setAmount(new BigDecimal("-50.00"));
        request.setCurrency("RON");
        request.setCategoryId(systemCategory.getCategoryId());
        request.setProcessingType("STANDARD");

        mockMvc.perform(post("/api/payments/initiate")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("18. Payment failure: unauthenticated request (401)")
    void testInitiatePaymentUnauthenticated() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setSourceAccountId(1L);
        request.setDestinationIban("RO99EXTR0000000000000099");
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("RON");
        request.setCategoryId(systemCategory.getCategoryId());
        request.setProcessingType("STANDARD");

        mockMvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // 2. OWN-ACCOUNT TRANSFERS
    // ==========================================

    @Test
    @DisplayName("19. Transfer between own accounts: success (EXECUTED)")
    void testTransferOwnSuccess() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont Sursa", "RON", new BigDecimal("2000.00"), "ACTIVE");
        AccountInternalSummaryDTO dest = new AccountInternalSummaryDTO(2L, "RO11BANK0000000000000002", "Cont Economii", "RON", new BigDecimal("500.00"), "ACTIVE");

        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.getAccount(2L)).thenReturn(dest);
        Mockito.when(accountClient.checkAccess(1L, 1, "CO_OWNER")).thenReturn(true);
        Mockito.when(accountClient.checkAccess(2L, 1, "VIEWER")).thenReturn(true);
        Mockito.when(accountClient.debit(eq(1L), any(BigDecimal.class), anyString())).thenReturn(source);
        Mockito.when(accountClient.credit(eq(2L), any(BigDecimal.class), anyString())).thenReturn(dest);

        OwnAccountTransferDTO request = new OwnAccountTransferDTO();
        request.setSourceAccountId(1L);
        request.setDestinationAccountId(2L);
        request.setAmount(new BigDecimal("400.00"));
        request.setCategoryId(systemCategory.getCategoryId());
        request.setDescription("Economii lunare");

        mockMvc.perform(post("/api/payments/transfer-own")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.amount").value(400.00))
                .andExpect(jsonPath("$.transactionType").value("INTERNAL"));

        Mockito.verify(accountClient, Mockito.times(1)).debit(eq(1L), eq(new BigDecimal("400.00")), anyString());
        Mockito.verify(accountClient, Mockito.times(1)).credit(eq(2L), eq(new BigDecimal("400.00")), anyString());
    }

    @Test
    @DisplayName("20. Transfer own failure: different currencies (400)")
    void testTransferOwnFailureDifferentCurrencies() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont RON", "RON", new BigDecimal("2000.00"), "ACTIVE");
        AccountInternalSummaryDTO dest = new AccountInternalSummaryDTO(2L, "RO11BANK0000000000000002", "Cont EUR", "EUR", new BigDecimal("500.00"), "ACTIVE");

        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.getAccount(2L)).thenReturn(dest);
        Mockito.when(accountClient.checkAccess(1L, 1, "CO_OWNER")).thenReturn(true);
        Mockito.when(accountClient.checkAccess(2L, 1, "VIEWER")).thenReturn(true);

        OwnAccountTransferDTO request = new OwnAccountTransferDTO();
        request.setSourceAccountId(1L);
        request.setDestinationAccountId(2L);
        request.setAmount(new BigDecimal("100.00"));
        request.setCategoryId(systemCategory.getCategoryId());

        mockMvc.perform(post("/api/payments/transfer-own")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("valut")));
    }

    @Test
    @DisplayName("21. Transfer own failure: same source and destination account (400)")
    void testTransferOwnFailureSameAccount() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont RON", "RON", new BigDecimal("2000.00"), "ACTIVE");

        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.checkAccess(1L, 1, "CO_OWNER")).thenReturn(true);
        Mockito.when(accountClient.checkAccess(1L, 1, "VIEWER")).thenReturn(true);

        OwnAccountTransferDTO request = new OwnAccountTransferDTO();
        request.setSourceAccountId(1L);
        request.setDestinationAccountId(1L);
        request.setAmount(new BigDecimal("100.00"));
        request.setCategoryId(systemCategory.getCategoryId());

        mockMvc.perform(post("/api/payments/transfer-own")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("diferite")));
    }

    @Test
    @DisplayName("22. Transfer own failure: destination not owned by user (403)")
    void testTransferOwnFailureDestinationNotOwned() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont Sursa", "RON", new BigDecimal("2000.00"), "ACTIVE");
        AccountInternalSummaryDTO dest = new AccountInternalSummaryDTO(2L, "RO11BANK0000000000000002", "Cont User2", "RON", new BigDecimal("500.00"), "ACTIVE");

        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.getAccount(2L)).thenReturn(dest);
        Mockito.when(accountClient.checkAccess(1L, 1, "CO_OWNER")).thenReturn(true);
        Mockito.when(accountClient.checkAccess(2L, 1, "VIEWER")).thenReturn(false); // User1 does not own Dest account

        OwnAccountTransferDTO request = new OwnAccountTransferDTO();
        request.setSourceAccountId(1L);
        request.setDestinationAccountId(2L);
        request.setAmount(new BigDecimal("100.00"));
        request.setCategoryId(systemCategory.getCategoryId());

        mockMvc.perform(post("/api/payments/transfer-own")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("23. Transfer own failure: insufficient funds (400)")
    void testTransferOwnFailureInsufficientFunds() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont Sursa", "RON", new BigDecimal("50.00"), "ACTIVE");
        AccountInternalSummaryDTO dest = new AccountInternalSummaryDTO(2L, "RO11BANK0000000000000002", "Cont Dest", "RON", new BigDecimal("500.00"), "ACTIVE");

        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.getAccount(2L)).thenReturn(dest);
        Mockito.when(accountClient.checkAccess(1L, 1, "CO_OWNER")).thenReturn(true);
        Mockito.when(accountClient.checkAccess(2L, 1, "VIEWER")).thenReturn(true);

        OwnAccountTransferDTO request = new OwnAccountTransferDTO();
        request.setSourceAccountId(1L);
        request.setDestinationAccountId(2L);
        request.setAmount(new BigDecimal("100.00"));
        request.setCategoryId(systemCategory.getCategoryId());

        mockMvc.perform(post("/api/payments/transfer-own")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("insuficiente")));
    }

    // ==========================================
    // 3. CURRENCY EXCHANGE
    // ==========================================

    @Test
    @DisplayName("24. Currency exchange: RON -> EUR with BigDecimal calculation")
    void testExchangeRonToEurSuccess() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont RON", "RON", new BigDecimal("5000.00"), "ACTIVE");
        AccountInternalSummaryDTO dest = new AccountInternalSummaryDTO(2L, "RO11BANK0000000000000002", "Cont EUR", "EUR", new BigDecimal("100.00"), "ACTIVE");

        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.getAccount(2L)).thenReturn(dest);
        Mockito.when(accountClient.checkAccess(1L, 1, "CO_OWNER")).thenReturn(true);
        Mockito.when(accountClient.checkAccess(2L, 1, "VIEWER")).thenReturn(true);

        CurrencyExchangeDTO request = new CurrencyExchangeDTO();
        request.setSourceAccountId(1L);
        request.setDestinationAccountId(2L);
        request.setAmount(new BigDecimal("497.50")); // 497.50 RON / 4.975000 = 100.00 EUR
        request.setCategoryId(systemCategory.getCategoryId());

        mockMvc.perform(post("/api/payments/exchange")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.transactionType").value("EXCHANGE"))
                .andExpect(jsonPath("$.amount").value(497.50));

        Mockito.verify(accountClient, Mockito.times(1)).debit(eq(1L), eq(new BigDecimal("497.50")), anyString());
        Mockito.verify(accountClient, Mockito.times(1)).credit(eq(2L), eq(new BigDecimal("100.00")), anyString());
    }

    @Test
    @DisplayName("25. Currency exchange: EUR -> RON with BigDecimal calculation")
    void testExchangeEurToRonSuccess() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(2L, "RO11BANK0000000000000002", "Cont EUR", "EUR", new BigDecimal("500.00"), "ACTIVE");
        AccountInternalSummaryDTO dest = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont RON", "RON", new BigDecimal("1000.00"), "ACTIVE");

        Mockito.when(accountClient.getAccount(2L)).thenReturn(source);
        Mockito.when(accountClient.getAccount(1L)).thenReturn(dest);
        Mockito.when(accountClient.checkAccess(2L, 1, "CO_OWNER")).thenReturn(true);
        Mockito.when(accountClient.checkAccess(1L, 1, "VIEWER")).thenReturn(true);

        CurrencyExchangeDTO request = new CurrencyExchangeDTO();
        request.setSourceAccountId(2L);
        request.setDestinationAccountId(1L);
        request.setAmount(new BigDecimal("100.00")); // 100.00 EUR * 4.975000 = 497.50 RON
        request.setCategoryId(systemCategory.getCategoryId());

        mockMvc.perform(post("/api/payments/exchange")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.transactionType").value("EXCHANGE"));

        Mockito.verify(accountClient, Mockito.times(1)).debit(eq(2L), eq(new BigDecimal("100.00")), anyString());
        Mockito.verify(accountClient, Mockito.times(1)).credit(eq(1L), eq(new BigDecimal("497.50")), anyString());
    }

    @Test
    @DisplayName("26. Currency exchange: USD -> RON with BigDecimal calculation")
    void testExchangeUsdToRonSuccess() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(3L, "RO11BANK0000000000000003", "Cont USD", "USD", new BigDecimal("500.00"), "ACTIVE");
        AccountInternalSummaryDTO dest = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont RON", "RON", new BigDecimal("1000.00"), "ACTIVE");

        Mockito.when(accountClient.getAccount(3L)).thenReturn(source);
        Mockito.when(accountClient.getAccount(1L)).thenReturn(dest);
        Mockito.when(accountClient.checkAccess(3L, 1, "CO_OWNER")).thenReturn(true);
        Mockito.when(accountClient.checkAccess(1L, 1, "VIEWER")).thenReturn(true);

        CurrencyExchangeDTO request = new CurrencyExchangeDTO();
        request.setSourceAccountId(3L);
        request.setDestinationAccountId(1L);
        request.setAmount(new BigDecimal("100.00")); // 100.00 USD * 4.550000 = 455.00 RON
        request.setCategoryId(systemCategory.getCategoryId());

        mockMvc.perform(post("/api/payments/exchange")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        Mockito.verify(accountClient, Mockito.times(1)).debit(eq(3L), eq(new BigDecimal("100.00")), anyString());
        Mockito.verify(accountClient, Mockito.times(1)).credit(eq(1L), eq(new BigDecimal("455.00")), anyString());
    }

    @Test
    @DisplayName("27. Currency exchange failure: non-RON pair EUR -> USD (400)")
    void testExchangeFailureNonRonPair() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(2L, "RO11BANK0000000000000002", "Cont EUR", "EUR", new BigDecimal("500.00"), "ACTIVE");
        AccountInternalSummaryDTO dest = new AccountInternalSummaryDTO(3L, "RO11BANK0000000000000003", "Cont USD", "USD", new BigDecimal("500.00"), "ACTIVE");

        Mockito.when(accountClient.getAccount(2L)).thenReturn(source);
        Mockito.when(accountClient.getAccount(3L)).thenReturn(dest);
        Mockito.when(accountClient.checkAccess(2L, 1, "CO_OWNER")).thenReturn(true);
        Mockito.when(accountClient.checkAccess(3L, 1, "VIEWER")).thenReturn(true);

        CurrencyExchangeDTO request = new CurrencyExchangeDTO();
        request.setSourceAccountId(2L);
        request.setDestinationAccountId(3L);
        request.setAmount(new BigDecimal("100.00"));
        request.setCategoryId(systemCategory.getCategoryId());

        mockMvc.perform(post("/api/payments/exchange")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("RON")));
    }

    @Test
    @DisplayName("28. Currency exchange failure: same currency (400)")
    void testExchangeFailureSameCurrency() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont RON 1", "RON", new BigDecimal("5000.00"), "ACTIVE");
        AccountInternalSummaryDTO dest = new AccountInternalSummaryDTO(2L, "RO11BANK0000000000000002", "Cont RON 2", "RON", new BigDecimal("100.00"), "ACTIVE");

        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.getAccount(2L)).thenReturn(dest);
        Mockito.when(accountClient.checkAccess(1L, 1, "CO_OWNER")).thenReturn(true);
        Mockito.when(accountClient.checkAccess(2L, 1, "VIEWER")).thenReturn(true);

        CurrencyExchangeDTO request = new CurrencyExchangeDTO();
        request.setSourceAccountId(1L);
        request.setDestinationAccountId(2L);
        request.setAmount(new BigDecimal("100.00"));
        request.setCategoryId(systemCategory.getCategoryId());

        mockMvc.perform(post("/api/payments/exchange")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("diferite")));
    }

    @Test
    @DisplayName("29. Currency exchange failure: insufficient funds (400)")
    void testExchangeFailureInsufficientFunds() throws Exception {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont RON", "RON", new BigDecimal("20.00"), "ACTIVE");
        AccountInternalSummaryDTO dest = new AccountInternalSummaryDTO(2L, "RO11BANK0000000000000002", "Cont EUR", "EUR", new BigDecimal("100.00"), "ACTIVE");

        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.getAccount(2L)).thenReturn(dest);
        Mockito.when(accountClient.checkAccess(1L, 1, "CO_OWNER")).thenReturn(true);
        Mockito.when(accountClient.checkAccess(2L, 1, "VIEWER")).thenReturn(true);

        CurrencyExchangeDTO request = new CurrencyExchangeDTO();
        request.setSourceAccountId(1L);
        request.setDestinationAccountId(2L);
        request.setAmount(new BigDecimal("100.00"));
        request.setCategoryId(systemCategory.getCategoryId());

        mockMvc.perform(post("/api/payments/exchange")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("insuficiente")));
    }

    // ==========================================
    // 4. TRANSACTION HISTORY & IDOR
    // ==========================================

    @Test
    @DisplayName("30. getTransactionsForUserPaged returns transactions for user")
    void testGetTransactionsForUserPaged() throws Exception {
        Transaction tx1 = new Transaction();
        tx1.setInitiatedByUserId(1);
        tx1.setSourceAccountId(1L);
        tx1.setAmount(new BigDecimal("100.00"));
        tx1.setCurrency("RON");
        tx1.setStatus("EXECUTED");
        tx1.setCreatedAt(new Date());
        transactionRepository.save(tx1);

        Transaction tx2 = new Transaction();
        tx2.setInitiatedByUserId(2);
        tx2.setSourceAccountId(20L);
        tx2.setAmount(new BigDecimal("500.00"));
        tx2.setCurrency("RON");
        tx2.setStatus("EXECUTED");
        tx2.setCreatedAt(new Date());
        transactionRepository.save(tx2);

        AccountInternalSummaryDTO user1Acc = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont RON", "RON", new BigDecimal("5000.00"), "ACTIVE");
        Mockito.when(accountClient.getUserAccounts(1)).thenReturn(List.of(user1Acc));

        mockMvc.perform(get("/api/transactions/user")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].amount").value(100.00));
    }

    @Test
    @DisplayName("31. getTransactionsForAccountPaged returns account transactions")
    void testGetTransactionsForAccountPaged() throws Exception {
        Transaction tx = new Transaction();
        tx.setInitiatedByUserId(1);
        tx.setSourceAccountId(1L);
        tx.setAmount(new BigDecimal("250.00"));
        tx.setCurrency("RON");
        tx.setStatus("EXECUTED");
        tx.setCreatedAt(new Date());
        transactionRepository.save(tx);

        AccountInternalSummaryDTO user1Acc = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont RON", "RON", new BigDecimal("5000.00"), "ACTIVE");
        Mockito.when(accountClient.getAccount(1L)).thenReturn(user1Acc);
        Mockito.when(accountClient.checkAccess(1L, 1, "VIEWER")).thenReturn(true);
        Mockito.when(accountClient.getUserAccounts(1)).thenReturn(List.of(user1Acc));

        mockMvc.perform(get("/api/transactions/account/1")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].amount").value(250.00));
    }

    @Test
    @DisplayName("32. getTransactionsForAccountPaged IDOR protection: User A cannot access User B's account history (403)")
    void testGetTransactionsForAccountIdorForbidden() throws Exception {
        AccountInternalSummaryDTO user2Acc = new AccountInternalSummaryDTO(20L, "RO11BANK0000000000000020", "Cont User2", "RON", new BigDecimal("5000.00"), "ACTIVE");
        Mockito.when(accountClient.getAccount(20L)).thenReturn(user2Acc);
        Mockito.when(accountClient.checkAccess(20L, 1, "VIEWER")).thenReturn(false);

        mockMvc.perform(get("/api/transactions/account/20")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("33. Transaction pagination and sorting work correctly")
    void testTransactionPaginationAndSorting() throws Exception {
        for (int i = 1; i <= 5; i++) {
            Transaction tx = new Transaction();
            tx.setInitiatedByUserId(1);
            tx.setSourceAccountId(1L);
            tx.setAmount(new BigDecimal(i * 10));
            tx.setCurrency("RON");
            tx.setStatus("EXECUTED");
            tx.setCreatedAt(new Date(System.currentTimeMillis() + i * 1000));
            transactionRepository.save(tx);
        }

        AccountInternalSummaryDTO user1Acc = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont RON", "RON", new BigDecimal("5000.00"), "ACTIVE");
        Mockito.when(accountClient.getAccount(1L)).thenReturn(user1Acc);
        Mockito.when(accountClient.checkAccess(1L, 1, "VIEWER")).thenReturn(true);
        Mockito.when(accountClient.getUserAccounts(1)).thenReturn(List.of(user1Acc));

        mockMvc.perform(get("/api/transactions/account/1?page=0&size=2&sortBy=amount&direction=asc")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].amount").value(10.00))
                .andExpect(jsonPath("$.content[1].amount").value(20.00))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    // ==========================================
    // 5. CATEGORIES CRUD & SECURITY
    // ==========================================

    @Test
    @DisplayName("34. Create new category for user (201)")
    void testCreateCategorySuccess() throws Exception {
        CategoryRequestDTO request = new CategoryRequestDTO();
        request.setName("Vacante");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Vacante"))
                .andExpect(jsonPath("$.isSystem").value("N"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("35. List available categories returns system and own categories")
    void testListAvailableCategories() throws Exception {
        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // systemCategory + user1Category
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Utilitati", "Personale User1")));
    }

    @Test
    @DisplayName("36. Get category by ID returns category")
    void testGetCategoryById() throws Exception {
        mockMvc.perform(get("/api/categories/" + user1Category.getCategoryId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Personale User1"));
    }

    @Test
    @DisplayName("37. Update own category (200)")
    void testUpdateOwnCategory() throws Exception {
        CategoryRequestDTO request = new CategoryRequestDTO();
        request.setName("Personale Modificate");

        mockMvc.perform(put("/api/categories/" + user1Category.getCategoryId())
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Category updated = categoryRepository.findById(user1Category.getCategoryId()).get();
        assertThat(updated.getName()).isEqualTo("Personale Modificate");
    }

    @Test
    @DisplayName("38. Delete own category performs soft-delete (INACTIVE, 204)")
    void testDeleteOwnCategory() throws Exception {
        mockMvc.perform(delete("/api/categories/" + user1Category.getCategoryId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNoContent());

        Category deleted = categoryRepository.findById(user1Category.getCategoryId()).get();
        assertThat(deleted.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    @DisplayName("39. Failure: delete system category (403)")
    void testDeleteSystemCategoryForbidden() throws Exception {
        mockMvc.perform(delete("/api/categories/" + systemCategory.getCategoryId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("40. Failure: modify another user's category (403)")
    void testModifyOtherUserCategoryForbidden() throws Exception {
        CategoryRequestDTO request = new CategoryRequestDTO();
        request.setName("Hacked Name");

        mockMvc.perform(put("/api/categories/" + user2Category.getCategoryId())
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("41. Failure: duplicate category name (400)")
    void testCreateDuplicateCategoryName() throws Exception {
        CategoryRequestDTO request = new CategoryRequestDTO();
        request.setName("Utilitati"); // already exists as system category

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("already exists")));
    }

    // ==========================================
    // 6. BACKGROUND JOBS
    // ==========================================

    @Test
    @DisplayName("42. PaymentJob processes PENDING_EXECUTION standard transactions")
    void testPaymentJobProcessesPending() {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont Sursa", "RON", new BigDecimal("5000.00"), "ACTIVE");
        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.debit(eq(1L), any(BigDecimal.class), anyString())).thenReturn(source);

        Transaction tx = new Transaction();
        tx.setInitiatedByUserId(1);
        tx.setSourceAccountId(1L);
        tx.setAmount(new BigDecimal("75.00"));
        tx.setCurrency("RON");
        tx.setStatus("PENDING_EXECUTION");
        tx.setIsUrgent("NO");
        tx.setIsScheduled("NO");
        tx.setCreatedAt(new Date());
        tx.setUpdatedAt(new Date());
        transactionRepository.save(tx);

        paymentJob.processStandardPayments();

        Transaction updated = transactionRepository.findById(tx.getTransactionId()).get();
        assertThat(updated.getStatus()).isEqualTo("EXECUTED");
        Mockito.verify(accountClient, Mockito.times(1)).debit(eq(1L), eq(new BigDecimal("75.00")), anyString());
    }

    @Test
    @DisplayName("43. ScheduledPaymentJob processes due scheduled payments")
    void testScheduledPaymentJobProcessesDue() {
        AccountInternalSummaryDTO source = new AccountInternalSummaryDTO(1L, "RO11BANK0000000000000001", "Cont Sursa", "RON", new BigDecimal("5000.00"), "ACTIVE");
        Mockito.when(accountClient.getAccount(1L)).thenReturn(source);
        Mockito.when(accountClient.debit(eq(1L), any(BigDecimal.class), anyString())).thenReturn(source);

        Transaction tx = new Transaction();
        tx.setInitiatedByUserId(1);
        tx.setSourceAccountId(1L);
        tx.setAmount(new BigDecimal("120.00"));
        tx.setCurrency("RON");
        tx.setStatus("PENDING_EXECUTION");
        tx.setIsUrgent("NO");
        tx.setIsScheduled("YES");
        tx.setCreatedAt(new Date());
        tx.setUpdatedAt(new Date());
        tx = transactionRepository.save(tx);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -1);

        ScheduledPayment sp = new ScheduledPayment();
        sp.setTransaction(tx);
        sp.setScheduledDate(cal.getTime());
        sp.setStatus("ACTIVE");
        sp.setCreatedAt(new Date());
        sp.setUpdatedAt(new Date());
        sp = scheduledPaymentRepository.save(sp);

        scheduledPaymentJob.processScheduledPayments();

        ScheduledPayment updatedSp = scheduledPaymentRepository.findById(sp.getScheduledPaymentId()).get();
        assertThat(updatedSp.getStatus()).isEqualTo("EXECUTED");

        Transaction updatedTx = transactionRepository.findById(tx.getTransactionId()).get();
        assertThat(updatedTx.getStatus()).isEqualTo("EXECUTED");
    }

    @Test
    @DisplayName("44. ExchangeRateUpdateJob executes without error")
    void testExchangeRateUpdateJobRunsGracefully() {
        exchangeRateUpdateJob.updateExchangeRates();
        List<ExchangeRate> rates = exchangeRateRepository.findAll();
        assertThat(rates).isNotEmpty();
    }
}
