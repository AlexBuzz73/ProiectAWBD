package com.example.demo.services;

import com.example.demo.domain.Account;
import com.example.demo.domain.AccountAccess;
import com.example.demo.domain.BankLimit;
import com.example.demo.domain.User;
import com.example.demo.dto.BankLimitUpdateDTO;
import com.example.demo.dto.SharedAccountRequest;
import com.example.demo.dto.UserRoleDTO;
import com.example.demo.repositories.AccountAccessRepository;
import com.example.demo.repositories.AccountRepository;
import com.example.demo.repositories.BankLimitRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.impl.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private AccountAccessRepository accountAccessRepository;
    @Mock private BankLimitRepository bankLimitRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User ownerUser;
    private User coOwnerUser;
    private Account account;

    @BeforeEach
    void setUp() {
        ownerUser = new User();
        ownerUser.setUserId(601);
        ownerUser.setEmail("owner@example.com");
        ownerUser.setStatus("ACTIVE");

        coOwnerUser = new User();
        coOwnerUser.setUserId(602);
        coOwnerUser.setEmail("coowner@example.com");
        coOwnerUser.setStatus("ACTIVE");

        account = new Account();
        account.setAccountId(801L);
        account.setAlias("Shared family account");
        account.setCurrency("RON");
        account.setStatus("ACTIVE");
    }

    private UserRoleDTO userRole(String email, String role) {
        UserRoleDTO dto = new UserRoleDTO();
        dto.setEmail(email);
        dto.setRole(role);
        return dto;
    }

    // ---------- createSharedAccount ----------

    @Test
    void createSharedAccount_validRequest_savesAccountAndAccessRows() {
        SharedAccountRequest request = new SharedAccountRequest();
        request.setAlias("Shared family account");
        request.setCurrency("RON");
        request.setUsers(List.of(
                userRole("owner@example.com", "OWNER"),
                userRole("coowner@example.com", "CO_OWNER")
        ));

        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            a.setAccountId(801L);
            return a;
        });
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(ownerUser));
        when(userRepository.findByEmail("coowner@example.com")).thenReturn(Optional.of(coOwnerUser));

        Account result = adminService.createSharedAccount(request);

        assertEquals("Shared family account", result.getAlias());
        assertEquals("ACTIVE", result.getStatus());
        verify(accountAccessRepository, times(2)).save(any(AccountAccess.class));
    }

    @Test
    void createSharedAccount_moreThanTwoUsers_throws() {
        SharedAccountRequest request = new SharedAccountRequest();
        request.setAlias("Too many owners");
        request.setCurrency("RON");
        request.setUsers(List.of(
                userRole("a@example.com", "OWNER"),
                userRole("b@example.com", "CO_OWNER"),
                userRole("c@example.com", "VIEWER")
        ));

        assertThrows(RuntimeException.class, () -> adminService.createSharedAccount(request));
        verifyNoInteractions(accountRepository);
    }

    @Test
    void createSharedAccount_noOwner_throws() {
        SharedAccountRequest request = new SharedAccountRequest();
        request.setAlias("No owner");
        request.setCurrency("RON");
        request.setUsers(List.of(userRole("coowner@example.com", "CO_OWNER")));

        assertThrows(RuntimeException.class, () -> adminService.createSharedAccount(request));
        verifyNoInteractions(accountRepository);
    }

    @Test
    void createSharedAccount_userEmailNotFound_throws() {
        SharedAccountRequest request = new SharedAccountRequest();
        request.setAlias("Unknown user");
        request.setCurrency("RON");
        request.setUsers(List.of(userRole("ghost@example.com", "OWNER")));

        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> adminService.createSharedAccount(request));
    }

    // ---------- updateBankLimits ----------

    @Test
    void updateBankLimits_existingActiveLimit_updatesValues() {
        BankLimit existing = new BankLimit();
        existing.setMaxAmountPerTransactionRon(BigDecimal.valueOf(1000));
        existing.setMaxDailyAmountRon(BigDecimal.valueOf(2000));
        existing.setMaxDailyTransactionsCount(BigDecimal.valueOf(5));
        existing.setStatus("ACTIVE");

        BankLimitUpdateDTO dto = new BankLimitUpdateDTO();
        dto.setMaxAmountPerTransactionRon(BigDecimal.valueOf(5000));
        dto.setMaxDailyAmountRon(BigDecimal.valueOf(10000));
        dto.setMaxDailyTransactionsCount(20);

        when(bankLimitRepository.findAll()).thenReturn(List.of(existing));
        when(bankLimitRepository.save(any(BankLimit.class))).thenAnswer(inv -> inv.getArgument(0));

        adminService.updateBankLimits(dto);

        assertEquals(BigDecimal.valueOf(5000), existing.getMaxAmountPerTransactionRon());
        assertEquals(BigDecimal.valueOf(10000), existing.getMaxDailyAmountRon());
        assertEquals(0, BigDecimal.valueOf(20).compareTo(existing.getMaxDailyTransactionsCount()));
    }

    // ---------- revokeAccountAccess ----------

    @Test
    void revokeAccountAccess_lastOwner_throws() {
        AccountAccess soleOwnerAccess = new AccountAccess();
        soleOwnerAccess.setUser(ownerUser);
        soleOwnerAccess.setAccount(account);
        soleOwnerAccess.setAccessRole("OWNER");
        soleOwnerAccess.setStatus("ACTIVE");

        when(accountAccessRepository.findByAccountAccountIdAndUserEmail(801L, "owner@example.com"))
                .thenReturn(Optional.of(soleOwnerAccess));
        when(accountAccessRepository.findByAccountAccountId(801L))
                .thenReturn(List.of(soleOwnerAccess));

        assertThrows(RuntimeException.class,
                () -> adminService.revokeAccountAccess(801L, "owner@example.com"));
        assertEquals("ACTIVE", soleOwnerAccess.getStatus());
    }

    @Test
    void revokeAccountAccess_coOwnerWithAnotherActiveOwner_setsInactive() {
        AccountAccess ownerAccess = new AccountAccess();
        ownerAccess.setUser(ownerUser);
        ownerAccess.setAccount(account);
        ownerAccess.setAccessRole("OWNER");
        ownerAccess.setStatus("ACTIVE");

        AccountAccess coOwnerAccess = new AccountAccess();
        coOwnerAccess.setUser(coOwnerUser);
        coOwnerAccess.setAccount(account);
        coOwnerAccess.setAccessRole("CO_OWNER");
        coOwnerAccess.setStatus("ACTIVE");

        when(accountAccessRepository.findByAccountAccountIdAndUserEmail(801L, "coowner@example.com"))
                .thenReturn(Optional.of(coOwnerAccess));
        when(accountAccessRepository.save(any(AccountAccess.class))).thenAnswer(inv -> inv.getArgument(0));

        adminService.revokeAccountAccess(801L, "coowner@example.com");

        assertEquals("INACTIVE", coOwnerAccess.getStatus());
    }

    @Test
    void revokeAccountAccess_accessNotFound_throws() {
        when(accountAccessRepository.findByAccountAccountIdAndUserEmail(801L, "missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> adminService.revokeAccountAccess(801L, "missing@example.com"));
    }
}
