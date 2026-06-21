package com.example.demo.services;

import com.example.demo.domain.*;
import com.example.demo.dto.CurrencyExchangeDTO;
import com.example.demo.dto.OwnAccountTransferDTO;
import com.example.demo.dto.PaymentRequestDTO;
import com.example.demo.mappers.TransactionMapper;
import com.example.demo.repositories.*;
import com.example.demo.services.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserLimitRepository userLimitRepository;
    @Mock private BankLimitRepository bankLimitRepository;
    @Mock private TagRepository tagRepository;
    @Mock private ScheduledPaymentRepository scheduledPaymentRepository;
    @Mock private ExchangeRateRepository exchangeRateRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AccountAccessRepository accountAccessRepository;
    @Mock private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    // Folosim ID-uri mari (peste 127) ca sa nu mascam eventuale bug-uri de comparare
    // Integer cu "==" prin cache-ul de autoboxing al lui Java.
    private User user;
    private Account sourceAccount;
    private Account destinationAccount;
    private AccountAccess ownerAccess;
    private AccountAccess viewerAccess;
    private Category category;
    private BankLimit bankLimit;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(501);
        user.setUsername("ana");
        user.setStatus("ACTIVE");
        user.setPasswordHash("hashed-pass");

        sourceAccount = new Account();
        sourceAccount.setAccountId(901L);
        sourceAccount.setIban("RO11BANK0000000000000001");
        sourceAccount.setCurrency("RON");
        sourceAccount.setBalance(1000.0);
        sourceAccount.setStatus("ACTIVE");
        sourceAccount.setAccountAccessList(new ArrayList<>());

        destinationAccount = new Account();
        destinationAccount.setAccountId(902L);
        destinationAccount.setIban("RO11BANK0000000000000002");
        destinationAccount.setCurrency("RON");
        destinationAccount.setBalance(200.0);
        destinationAccount.setStatus("ACTIVE");
        destinationAccount.setAccountAccessList(new ArrayList<>());

        ownerAccess = new AccountAccess();
        ownerAccess.setUser(user);
        ownerAccess.setAccount(sourceAccount);
        ownerAccess.setAccessRole("OWNER");
        ownerAccess.setStatus("ACTIVE");
        sourceAccount.getAccountAccessList().add(ownerAccess);

        viewerAccess = new AccountAccess();
        viewerAccess.setUser(user);
        viewerAccess.setAccount(sourceAccount);
        viewerAccess.setAccessRole("VIEWER");
        viewerAccess.setStatus("ACTIVE");

        category = new Category();
        category.setCategoryId(701);
        category.setName("Utilities");

        bankLimit = new BankLimit();
        bankLimit.setMaxAmountPerTransactionRon(BigDecimal.valueOf(5000));
        bankLimit.setMaxDailyAmountRon(BigDecimal.valueOf(10000));
        bankLimit.setMaxDailyTransactionsCount(BigDecimal.valueOf(20));
    }

    private PaymentRequestDTO buildPaymentDto(String processingType, Date scheduledDate) {
        PaymentRequestDTO dto = new PaymentRequestDTO();
        dto.setSourceAccountId(901);
        dto.setDestinationIban("RO11BANK0000000000000099");
        dto.setAmount(100.0);
        dto.setCurrency("RON");
        dto.setCategoryId(701);
        dto.setProcessingType(processingType);
        dto.setScheduledDate(scheduledDate);
        dto.setDescription("test payment");
        dto.setPassword("correct-password");
        return dto;
    }

    // ---------- initiatePayment ----------

    @Test
    void initiatePayment_standardExternalPayment_endsInPendingExecution() {
        PaymentRequestDTO dto = buildPaymentDto("STANDARD", null);
        java.util.concurrent.atomic.AtomicReference<Transaction> savedRef = new java.util.concurrent.atomic.AtomicReference<>();

        when(passwordEncoder.matches("correct-password", "hashed-pass")).thenReturn(true);
        when(accountRepository.findById(901L)).thenReturn(Optional.of(sourceAccount));
        when(accountAccessRepository.findByAccountAccountIdAndUserUserIdAndStatus(901L, 501, "ACTIVE"))
                .thenReturn(Optional.of(ownerAccess));
        when(accountRepository.findByIban(dto.getDestinationIban())).thenReturn(Optional.empty());
        when(categoryRepository.findById(701)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            if (t.getTransactionId() == 0) {
                t.setTransactionId(999);
            }
            savedRef.set(t);
            return t;
        });
        when(transactionRepository.findById(999)).thenAnswer(inv -> Optional.ofNullable(savedRef.get()));
        when(userLimitRepository.findByUserUserIdAndStatus(501, "ACTIVE")).thenReturn(Optional.empty());
        when(bankLimitRepository.findAll()).thenReturn(List.of(bankLimit));

        Transaction result = transactionService.initiatePayment(dto, user);

        assertEquals("PENDING_EXECUTION", result.getStatus());
        assertEquals("EXTERNAL", result.getTransactionType());
        assertEquals("NO", result.getIsUrgent());
        assertEquals("NO", result.getIsScheduled());
        verify(scheduledPaymentRepository, never()).save(any());
    }

    @Test
    void initiatePayment_wrongPassword_throws() {
        PaymentRequestDTO dto = buildPaymentDto("STANDARD", null);
        when(passwordEncoder.matches("correct-password", "hashed-pass")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> transactionService.initiatePayment(dto, user));
        verifyNoInteractions(accountRepository);
    }

    @Test
    void initiatePayment_inactiveSourceAccount_throws() {
        PaymentRequestDTO dto = buildPaymentDto("STANDARD", null);
        sourceAccount.setStatus("BLOCKED");

        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(accountRepository.findById(901L)).thenReturn(Optional.of(sourceAccount));

        assertThrows(RuntimeException.class, () -> transactionService.initiatePayment(dto, user));
    }

    @Test
    void initiatePayment_viewerRole_throws() {
        PaymentRequestDTO dto = buildPaymentDto("STANDARD", null);

        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(accountRepository.findById(901L)).thenReturn(Optional.of(sourceAccount));
        when(accountAccessRepository.findByAccountAccountIdAndUserUserIdAndStatus(901L, 501, "ACTIVE"))
                .thenReturn(Optional.of(viewerAccess));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transactionService.initiatePayment(dto, user));
        assertTrue(ex.getMessage().contains("VIEWER"));
    }

    @Test
    void initiatePayment_currencyMismatch_throws() {
        PaymentRequestDTO dto = buildPaymentDto("STANDARD", null);
        dto.setCurrency("EUR");

        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(accountRepository.findById(901L)).thenReturn(Optional.of(sourceAccount));

        assertThrows(RuntimeException.class, () -> transactionService.initiatePayment(dto, user));
    }

    @Test
    void initiatePayment_scheduledInThePast_throws() {
        Date yesterday = new Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000);
        PaymentRequestDTO dto = buildPaymentDto("PROGRAMAT", yesterday);

        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThrows(RuntimeException.class, () -> transactionService.initiatePayment(dto, user));
    }

    @Test
    void initiatePayment_scheduledInTheFuture_createsScheduledPaymentRow() {
        Date tomorrow = new Date(System.currentTimeMillis() + 24L * 60 * 60 * 1000);
        PaymentRequestDTO dto = buildPaymentDto("PROGRAMAT", tomorrow);
        java.util.concurrent.atomic.AtomicReference<Transaction> savedRef = new java.util.concurrent.atomic.AtomicReference<>();

        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(accountRepository.findById(901L)).thenReturn(Optional.of(sourceAccount));
        when(accountAccessRepository.findByAccountAccountIdAndUserUserIdAndStatus(901L, 501, "ACTIVE"))
                .thenReturn(Optional.of(ownerAccess));
        when(accountRepository.findByIban(dto.getDestinationIban())).thenReturn(Optional.empty());
        when(categoryRepository.findById(701)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            if (t.getTransactionId() == 0) {
                t.setTransactionId(998);
            }
            savedRef.set(t);
            return t;
        });
        when(transactionRepository.findById(998)).thenAnswer(inv -> Optional.ofNullable(savedRef.get()));
        when(userLimitRepository.findByUserUserIdAndStatus(501, "ACTIVE")).thenReturn(Optional.empty());
        when(bankLimitRepository.findAll()).thenReturn(List.of(bankLimit));

        Transaction result = transactionService.initiatePayment(dto, user);

        assertEquals("YES", result.getIsScheduled());
        assertEquals("PENDING_EXECUTION", result.getStatus());
        verify(scheduledPaymentRepository, times(1)).save(any(ScheduledPayment.class));
    }

    // ---------- authorizePayment ----------

    @Test
    void authorizePayment_standardPayment_movesToPendingExecution() {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(1);
        transaction.setStatus("DRAFT");
        transaction.setAmount(100.0);
        transaction.setSourceAccount(sourceAccount);
        transaction.setIsUrgent("NO");

        when(transactionRepository.findById(1)).thenReturn(Optional.of(transaction));
        when(passwordEncoder.matches("pass", "hashed-pass")).thenReturn(true);
        when(userLimitRepository.findByUserUserIdAndStatus(501, "ACTIVE")).thenReturn(Optional.empty());
        when(bankLimitRepository.findAll()).thenReturn(List.of(bankLimit));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction result = transactionService.authorizePayment(1, "pass", user);

        assertEquals("PENDING_EXECUTION", result.getStatus());
    }

    @Test
    void authorizePayment_urgentPayment_executesImmediately() {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(2);
        transaction.setStatus("DRAFT");
        transaction.setAmount(100.0);
        transaction.setSourceAccount(sourceAccount);
        transaction.setTransactionType("EXTERNAL");
        transaction.setIsUrgent("YES");

        when(transactionRepository.findById(2)).thenReturn(Optional.of(transaction));
        when(passwordEncoder.matches("pass", "hashed-pass")).thenReturn(true);
        when(userLimitRepository.findByUserUserIdAndStatus(501, "ACTIVE")).thenReturn(Optional.empty());
        when(bankLimitRepository.findAll()).thenReturn(List.of(bankLimit));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction result = transactionService.authorizePayment(2, "pass", user);

        assertEquals("EXECUTED", result.getStatus());
        assertEquals(900.0, sourceAccount.getBalance());
    }

    @Test
    void authorizePayment_insufficientFunds_marksFailedAndThrows() {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(3);
        transaction.setStatus("DRAFT");
        transaction.setAmount(5000.0);
        transaction.setSourceAccount(sourceAccount);

        when(transactionRepository.findById(3)).thenReturn(Optional.of(transaction));
        when(passwordEncoder.matches("pass", "hashed-pass")).thenReturn(true);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(RuntimeException.class, () -> transactionService.authorizePayment(3, "pass", user));
        assertEquals("FAILED", transaction.getStatus());
    }

    @Test
    void authorizePayment_notInDraftStatus_throws() {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(4);
        transaction.setStatus("EXECUTED");

        when(transactionRepository.findById(4)).thenReturn(Optional.of(transaction));

        assertThrows(RuntimeException.class, () -> transactionService.authorizePayment(4, "pass", user));
    }

    // ---------- executeTransaction ----------

    @Test
    void executeTransaction_internalTransaction_updatesBothBalances() {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(5);
        transaction.setAmount(150.0);
        transaction.setSourceAccount(sourceAccount);
        transaction.setDestinationAccount(destinationAccount);
        transaction.setTransactionType("INTERNAL");

        when(transactionRepository.findById(5)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.executeTransaction(5);

        assertEquals(850.0, sourceAccount.getBalance());
        assertEquals(350.0, destinationAccount.getBalance());
        assertEquals("EXECUTED", transaction.getStatus());
    }

    @Test
    void executeTransaction_inactiveSourceAccount_marksFailedAndThrows() {
        sourceAccount.setStatus("BLOCKED");
        Transaction transaction = new Transaction();
        transaction.setTransactionId(6);
        transaction.setAmount(50.0);
        transaction.setSourceAccount(sourceAccount);

        when(transactionRepository.findById(6)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(RuntimeException.class, () -> transactionService.executeTransaction(6));
        assertEquals("FAILED", transaction.getStatus());
    }

    // ---------- transferBetweenOwnAccounts ----------

    private OwnAccountTransferDTO buildTransferDto() {
        OwnAccountTransferDTO dto = new OwnAccountTransferDTO();
        dto.setSourceAccountId(901);
        dto.setDestinationAccountId(902);
        dto.setAmount(100.0);
        dto.setCategoryId(701);
        dto.setDescription("own transfer");
        dto.setPassword("correct-password");
        return dto;
    }

    @Test
    void transferBetweenOwnAccounts_success_updatesBalancesAndCreatesExecutedTransaction() {
        AccountAccess destOwnerAccess = new AccountAccess();
        destOwnerAccess.setUser(user);
        destOwnerAccess.setAccount(destinationAccount);
        destOwnerAccess.setAccessRole("OWNER");
        destOwnerAccess.setStatus("ACTIVE");
        destinationAccount.getAccountAccessList().add(destOwnerAccess);

        OwnAccountTransferDTO dto = buildTransferDto();

        when(passwordEncoder.matches("correct-password", "hashed-pass")).thenReturn(true);
        when(accountRepository.findById(901L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(902L)).thenReturn(Optional.of(destinationAccount));
        when(userLimitRepository.findByUserUserIdAndStatus(501, "ACTIVE")).thenReturn(Optional.empty());
        when(bankLimitRepository.findAll()).thenReturn(List.of(bankLimit));
        when(categoryRepository.findById(701)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction result = transactionService.transferBetweenOwnAccounts(dto, user);

        assertEquals("EXECUTED", result.getStatus());
        assertEquals(900.0, sourceAccount.getBalance());
        assertEquals(300.0, destinationAccount.getBalance());
    }

    @Test
    void transferBetweenOwnAccounts_destinationNotOwnedByUser_throws() {
        // destinationAccount nu are nicio intrare AccountAccess pentru acest user
        OwnAccountTransferDTO dto = buildTransferDto();

        when(passwordEncoder.matches("correct-password", "hashed-pass")).thenReturn(true);
        when(accountRepository.findById(901L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(902L)).thenReturn(Optional.of(destinationAccount));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transactionService.transferBetweenOwnAccounts(dto, user));
        assertTrue(ex.getMessage().contains("aparțină"));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void transferBetweenOwnAccounts_differentCurrency_throws() {
        destinationAccount.setCurrency("EUR");
        OwnAccountTransferDTO dto = buildTransferDto();

        when(passwordEncoder.matches("correct-password", "hashed-pass")).thenReturn(true);
        when(accountRepository.findById(901L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(902L)).thenReturn(Optional.of(destinationAccount));

        assertThrows(RuntimeException.class, () -> transactionService.transferBetweenOwnAccounts(dto, user));
    }

    // ---------- performCurrencyExchange ----------

    private CurrencyExchangeDTO buildExchangeDto() {
        CurrencyExchangeDTO dto = new CurrencyExchangeDTO();
        dto.setSourceAccountId(901);
        dto.setDestinationAccountId(902);
        dto.setAmount(100.0);
        dto.setCategoryId(701);
        dto.setDescription("exchange");
        dto.setPassword("correct-password");
        return dto;
    }

    @Test
    void performCurrencyExchange_ronToUsd_convertsAmountUsingRate() {
        destinationAccount.setCurrency("USD");
        AccountAccess destOwnerAccess = new AccountAccess();
        destOwnerAccess.setUser(user);
        destOwnerAccess.setAccount(destinationAccount);
        destOwnerAccess.setAccessRole("OWNER");
        destOwnerAccess.setStatus("ACTIVE");
        destinationAccount.getAccountAccessList().add(destOwnerAccess);

        ExchangeRate rate = new ExchangeRate();
        rate.setCurrencyFrom("USD");
        rate.setCurrencyTo("RON");
        rate.setRate(4.5);

        CurrencyExchangeDTO dto = buildExchangeDto();

        when(passwordEncoder.matches("correct-password", "hashed-pass")).thenReturn(true);
        when(accountRepository.findById(901L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(902L)).thenReturn(Optional.of(destinationAccount));
        when(exchangeRateRepository.findTopByCurrencyFromAndCurrencyToOrderByRateDateDesc("USD", "RON"))
                .thenReturn(Optional.of(rate));
        when(userLimitRepository.findByUserUserIdAndStatus(501, "ACTIVE")).thenReturn(Optional.empty());
        when(bankLimitRepository.findAll()).thenReturn(List.of(bankLimit));
        when(categoryRepository.findById(701)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction result = transactionService.performCurrencyExchange(dto, user);

        // RON -> USD: amount_converted = amount / rate
        assertEquals(900.0, sourceAccount.getBalance(), 0.001);
        assertEquals(200.0 + (100.0 / 4.5), destinationAccount.getBalance(), 0.001);
        assertEquals("EXECUTED", result.getStatus());
        assertEquals("EXCHANGE", result.getTransactionType());
    }

    @Test
    void performCurrencyExchange_destinationNotOwnedByUser_throws() {
        destinationAccount.setCurrency("USD");
        CurrencyExchangeDTO dto = buildExchangeDto();

        when(passwordEncoder.matches("correct-password", "hashed-pass")).thenReturn(true);
        when(accountRepository.findById(901L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(902L)).thenReturn(Optional.of(destinationAccount));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transactionService.performCurrencyExchange(dto, user));
        assertTrue(ex.getMessage().contains("aparțină"));
    }

    @Test
    void performCurrencyExchange_unsupportedPair_throws() {
        sourceAccount.setCurrency("USD");
        destinationAccount.setCurrency("EUR");

        AccountAccess destOwnerAccess = new AccountAccess();
        destOwnerAccess.setUser(user);
        destOwnerAccess.setAccount(destinationAccount);
        destOwnerAccess.setAccessRole("OWNER");
        destOwnerAccess.setStatus("ACTIVE");
        destinationAccount.getAccountAccessList().add(destOwnerAccess);

        CurrencyExchangeDTO dto = buildExchangeDto();

        when(passwordEncoder.matches("correct-password", "hashed-pass")).thenReturn(true);
        when(accountRepository.findById(901L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(902L)).thenReturn(Optional.of(destinationAccount));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transactionService.performCurrencyExchange(dto, user));
        assertTrue(ex.getMessage().toLowerCase().contains("ron"));
    }
}
