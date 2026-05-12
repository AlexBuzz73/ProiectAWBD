package com.example.demo.services;

import com.example.demo.domain.Account;
import com.example.demo.domain.AccountAccess;
import com.example.demo.domain.Transaction;
import com.example.demo.domain.User;
import com.example.demo.dto.AccountDetailsDTO;
import com.example.demo.dto.AccountResponseDTO;
import com.example.demo.dto.AccountSummaryDTO;
import com.example.demo.dto.CreateSingleAccountRequestDTO;
import com.example.demo.mappers.AccountMapper;
import com.example.demo.repositories.AccountAccessRepository;
import com.example.demo.repositories.AccountRepository;
import com.example.demo.repositories.TransactionRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.impl.AccountServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountAccessRepository accountAccessRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountServiceImpl accountService;

    private CreateSingleAccountRequestDTO validCreateAccountRequest() {
        CreateSingleAccountRequestDTO dto = new CreateSingleAccountRequestDTO();
        dto.setAlias("Savings Account");
        dto.setCurrency("RON");
        dto.setExternalIban("RO49AAAA1B31007593840000");
        dto.setInitialAmount(5000);

        return dto;
    }

    private User validUser() {
        User user = new User();
        user.setUserId(1);
        user.setUsername("alex123");
        user.setEmail("alex@test.com");
        user.setStatus("ACTIVE");

        return user;
    }

    private Account validAccount() {
        Account account = new Account();
        account.setAccountId(1L);
        account.setAlias("Savings Account");
        account.setCurrency("RON");
        account.setIban("RO11BANK1234567890123456");
        account.setBalance(0);
        account.setStatus("ACTIVE");

        return account;
    }

    private AccountAccess validAccountAccess(User user, Account account) {
        AccountAccess access = new AccountAccess();
        access.setAccountAccessId(1L);
        access.setUser(user);
        access.setAccount(account);
        access.setAccessRole("OWNER");
        access.setStatus("ACTIVE");

        return access;
    }

    private AccountResponseDTO validAccountResponseDTO() {
        return new AccountResponseDTO(1L, "Savings Account", "RO11BANK1234567890123456", "RON", 5000, "ACTIVE");
    }

    private AccountSummaryDTO validAccountSummaryDTO() {
        return new AccountSummaryDTO(1L, "Savings Account", "RO11BANK1234567890123456", "RON", 5000, "OWNER");
    }

    private AccountDetailsDTO validAccountDetailsDTO() {
        return new AccountDetailsDTO(1L, "Savings Account", "RO11BANK1234567890123456", "RON", 5000, "ACTIVE", "OWNER", true);
    }


    @Test
    void createSingleAccount_shouldCreateAccountAccessAndTopUpTransaction_whenRequestIsValid() {
        CreateSingleAccountRequestDTO request = validCreateAccountRequest();

        User user = validUser();
        Account account = validAccount();
        AccountResponseDTO responseDTO = validAccountResponseDTO();

        when(userRepository.findById(user.getUserId()))
                .thenReturn(Optional.of(user));

        when(accountMapper.toAccount(request))
                .thenReturn(account);

        when(accountRepository.save(any(Account.class)))
                .thenReturn(account);

        when(accountMapper.toAccountResponseDTO(account))
                .thenReturn(responseDTO);

        AccountResponseDTO response = accountService.createSingleAccount(request, user.getUserId());

        assertNotNull(response);
        assertEquals(1L, response.getAccountId());
        assertEquals("RON", response.getCurrency());
        assertEquals(5000, response.getBalance());

        verify(accountRepository, atLeastOnce()).save(any(Account.class));
        verify(accountAccessRepository, times(1)).save(any(AccountAccess.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void createSingleAccount_shouldThrowException_whenAliasIsMissing() {
        CreateSingleAccountRequestDTO request = validCreateAccountRequest();
        request.setAlias("");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> accountService.createSingleAccount(request, 1));
        assertEquals("Account alias is required!", exception.getMessage());
    }

    @Test
    void createSingleAccount_shouldThrowException_whenCurrencyIsMissing() {
        CreateSingleAccountRequestDTO request = validCreateAccountRequest();
        request.setCurrency("");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> accountService.createSingleAccount(request, 1));
        assertEquals("Account currency is required!", exception.getMessage());
    }

    @Test
    void createSingleAccount_shouldThrowException_whenCurrencyIsUnsupported() {
        CreateSingleAccountRequestDTO request = validCreateAccountRequest();
        request.setCurrency("XAU");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> accountService.createSingleAccount(request, 1));
        assertEquals("Unsupported currency!", exception.getMessage());
    }

    @Test
    void createSingleAccount_shouldThrowException_whenInitialAmountIsNotPositive() {
        CreateSingleAccountRequestDTO request = validCreateAccountRequest();
        request.setInitialAmount(0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> accountService.createSingleAccount(request, 1));
        assertEquals("Initial amount must be positive!", exception.getMessage());
    }

    @Test
    void createSingleAccount_shouldThrowException_whenExternalIbanIsMissing() {
        CreateSingleAccountRequestDTO request = validCreateAccountRequest();
        request.setExternalIban("");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> accountService.createSingleAccount(request, 1));
        assertEquals("External IBAN is required!", exception.getMessage());
    }

    @Test
    void createSingleAccount_shouldThrowException_whenUserDoesNotExist() {
        CreateSingleAccountRequestDTO request = validCreateAccountRequest();

        when(userRepository.findById(1))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> accountService.createSingleAccount(request, 1));
        assertEquals("User not found!", exception.getMessage());

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void createSingleAccount_shouldThrowException_whenUserIsNotActive() {
        CreateSingleAccountRequestDTO request = validCreateAccountRequest();

        User user = validUser();
        user.setStatus("BLOCKED"); // sau CLOSED

        when(userRepository.findById(user.getUserId()))
                .thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> accountService.createSingleAccount(request, user.getUserId()));
        assertEquals("User is not active!", exception.getMessage());

        verify(accountRepository, never()).save(any(Account.class));
    }


    @Test
    void getActiveAccountsForUser_shouldReturnOnlyActiveAccounts_whenUserIsActive() {
        User user = validUser();

        Account activeAccount = validAccount();
        Account closedAccount = new Account();
        closedAccount.setAccountId(2L);
        closedAccount.setStatus("CLOSED");

        AccountAccess activeAccess = validAccountAccess(user, activeAccount);
        AccountAccess closedAccess = validAccountAccess(user, closedAccount);

        AccountSummaryDTO summaryDTO = validAccountSummaryDTO();

        when(userRepository.findById(user.getUserId()))
                .thenReturn(Optional.of(user));

        when(accountAccessRepository.findByUserUserIdAndStatus(user.getUserId(), "ACTIVE"))
                .thenReturn(List.of(activeAccess, closedAccess));

        when(accountMapper.toAccountSummaryDTO(activeAccount, activeAccess))
                .thenReturn(summaryDTO);

        List<AccountSummaryDTO> result = accountService.getActiveAccountsForUser(user.getUserId());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getAccountId());

        verify(accountMapper, times(1)).toAccountSummaryDTO(activeAccount, activeAccess);
    }

    @Test
    void getActiveAccountsForUser_shouldThrowException_whenUserDoesNotExist() {
        when(userRepository.findById(1))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> accountService.getActiveAccountsForUser(1));
        assertEquals("User not found!", exception.getMessage());

        verify(accountAccessRepository, never()).findByUserUserIdAndStatus(anyInt(), anyString());
    }


    @Test
    void getActiveAccountsForUser_shouldThrowException_whenUserIsNotActive() {
        User user = validUser();
        user.setStatus("BLOCKED");

        when(userRepository.findById(user.getUserId()))
                .thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> accountService.getActiveAccountsForUser(user.getUserId()));
        assertEquals("User is not active!", exception.getMessage());

        verify(accountAccessRepository, never()).findByUserUserIdAndStatus(anyInt(), anyString());
    }


    @Test
    void getAccountDetails_shouldReturnAccountDetails_whenUserHasActiveAccess() {
        User user = validUser();
        Account account = validAccount();
        AccountAccess access = validAccountAccess(user, account);
        AccountDetailsDTO detailsDTO = validAccountDetailsDTO();

        when(userRepository.findById(user.getUserId()))
                .thenReturn(Optional.of(user));

        when(accountAccessRepository.findByAccountAccountIdAndUserUserIdAndStatus(account.getAccountId(), user.getUserId(), "ACTIVE"))
                .thenReturn(Optional.of(access));

        when(accountMapper.toAccountDetailsDTO(account, access))
                .thenReturn(detailsDTO);

        AccountDetailsDTO result = accountService.getAccountDetails(account.getAccountId(), user.getUserId());

        assertNotNull(result);
        assertEquals(1L, result.getAccountId());
        assertEquals("OWNER", result.getAccountRole());
        assertTrue(result.isCanInitiateTransactions());

        verify(accountMapper, times(1)).toAccountDetailsDTO(account, access);
    }

    @Test
    void getAccountDetails_shouldThrowException_whenUserDoesNotExist() {
        when(userRepository.findById(1))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> accountService.getAccountDetails(1L, 1));
        assertEquals("User not found!", exception.getMessage());
    }

    @Test
    void getAccountDetails_shouldThrowException_whenUserIsNotActive() {
        User user = validUser();
        user.setStatus("BLOCKED");

        when(userRepository.findById(user.getUserId()))
                .thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> accountService.getAccountDetails(1L, user.getUserId()));
        assertEquals("User is not active!", exception.getMessage());
    }

    @Test
    void getAccountDetails_shouldThrowException_whenUserHasNoAccessToAccount() {
        User user = validUser();

        when(userRepository.findById(user.getUserId()))
                .thenReturn(Optional.of(user));

        when(accountAccessRepository.findByAccountAccountIdAndUserUserIdAndStatus(1L, user.getUserId(), "ACTIVE"))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> accountService.getAccountDetails(1L, user.getUserId()));
        assertEquals("You don't have access to this account!", exception.getMessage());
    }


    @Test
    void closeAccount_shouldCloseAccount_whenUserIsOwnerAndBalanceIsZero() {
        User user = validUser();

        Account account = validAccount();
        account.setBalance(0);
        account.setStatus("ACTIVE");

        AccountAccess access = validAccountAccess(user, account);
        access.setAccessRole("OWNER");

        when(userRepository.findById(user.getUserId()))
                .thenReturn(Optional.of(user));

        when(accountAccessRepository.findByAccountAccountIdAndUserUserIdAndStatus(account.getAccountId(), user.getUserId(), "ACTIVE"))
                .thenReturn(Optional.of(access));

        accountService.closeAccount(account.getAccountId(), user.getUserId());

        assertEquals("CLOSED", account.getStatus());

        verify(accountRepository, times(1)).save(account);
    }

    @Test
    void closeAccount_shouldThrowException_whenUserDoesNotExist() {
        when(userRepository.findById(1))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> accountService.closeAccount(1L, 1));
        assertEquals("User not found!", exception.getMessage());
    }

    @Test
    void closeAccount_shouldThrowException_whenUserIsNotActive() {
        User user = validUser();
        user.setStatus("BLOCKED");

        when(userRepository.findById(user.getUserId()))
                .thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> accountService.closeAccount(1L, user.getUserId()));
        assertEquals("User is not active!", exception.getMessage());
    }

    @Test
    void closeAccount_shouldThrowException_whenUserHasNoAccessToAccount() {
        User user = validUser();

        when(userRepository.findById(user.getUserId()))
                .thenReturn(Optional.of(user));

        when(accountAccessRepository.findByAccountAccountIdAndUserUserIdAndStatus(1L, user.getUserId(), "ACTIVE"))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> accountService.closeAccount(1L, user.getUserId()));
        assertEquals("You don't have access to this account!", exception.getMessage());
    }

    @Test
    void closeAccount_shouldThrowException_whenUserIsNotOwner() {
        User user = validUser();

        Account account = validAccount();

        AccountAccess access = validAccountAccess(user, account);
        access.setAccessRole("VIEWER");

        when(userRepository.findById(user.getUserId()))
                .thenReturn(Optional.of(user));

        when(accountAccessRepository.findByAccountAccountIdAndUserUserIdAndStatus(account.getAccountId(), user.getUserId(), "ACTIVE"))
                .thenReturn(Optional.of(access));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> accountService.closeAccount(account.getAccountId(), user.getUserId()));
        assertEquals("Only the OWNER can close this account!", exception.getMessage());
    }

    @Test
    void closeAccount_shouldThrowException_whenAccountIsNotActive() {
        User user = validUser();

        Account account = validAccount();
        account.setStatus("CLOSED");

        AccountAccess access = validAccountAccess(user, account);

        when(userRepository.findById(user.getUserId()))
                .thenReturn(Optional.of(user));

        when(accountAccessRepository.findByAccountAccountIdAndUserUserIdAndStatus(account.getAccountId(), user.getUserId(), "ACTIVE"))
                .thenReturn(Optional.of(access));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> accountService.closeAccount(account.getAccountId(), user.getUserId()));
        assertEquals("Only ACTIVE accounts can be closed!", exception.getMessage());
    }

    @Test
    void closeAccount_shouldThrowException_whenAccountBalanceIsNotZero() {
        User user = validUser();

        Account account = validAccount();
        account.setBalance(100);

        AccountAccess access = validAccountAccess(user, account);

        when(userRepository.findById(user.getUserId()))
                .thenReturn(Optional.of(user));

        when(accountAccessRepository.findByAccountAccountIdAndUserUserIdAndStatus(account.getAccountId(), user.getUserId(), "ACTIVE"))
                .thenReturn(Optional.of(access));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> accountService.closeAccount(account.getAccountId(), user.getUserId()));
        assertEquals("Account balance must be 0 to close the account!", exception.getMessage());
    }
}
