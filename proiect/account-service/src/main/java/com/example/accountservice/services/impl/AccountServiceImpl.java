package com.example.accountservice.services.impl;

import com.example.accountservice.client.UserClient;
import com.example.accountservice.domain.Account;
import com.example.accountservice.domain.AccountAccess;
import com.example.accountservice.dto.*;
import com.example.accountservice.exceptions.ResourceNotFoundException;
import com.example.accountservice.mappers.AccountMapper;
import com.example.accountservice.repositories.AccountAccessRepository;
import com.example.accountservice.repositories.AccountRepository;
import com.example.accountservice.services.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountAccessRepository accountAccessRepository;
    private final AccountMapper accountMapper;
    private final UserClient userClient;

    @Override
    @Transactional
    public AccountResponseDTO createSingleAccount(CreateSingleAccountRequestDTO dto, Integer userId) {
        validateCreateSingleAccountRequest(dto);

        Account account = accountMapper.toAccount(dto);
        account.setIban(generateUniqueIban());
        Account savedAccount = accountRepository.save(account);

        AccountAccess access = new AccountAccess();
        access.setAccount(savedAccount);
        access.setUserId(userId);
        access.setAccessRole("OWNER");
        access.setStatus("ACTIVE");
        access.setCreatedAt(new Date());
        access.setUpdatedAt(new Date());
        accountAccessRepository.save(access);

        log.info("Created account {} with IBAN {} for userId {}", savedAccount.getAccountId(), savedAccount.getIban(), userId);
        return accountMapper.toAccountResponseDTO(savedAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountSummaryDTO> getActiveAccountsForUser(Integer userId) {
        List<AccountAccess> accesses = accountAccessRepository.findByUserIdAndStatus(userId, "ACTIVE");
        return accesses.stream()
                .filter(access -> "ACTIVE".equalsIgnoreCase(access.getAccount().getStatus()))
                .map(access -> accountMapper.toAccountSummaryDTO(access.getAccount(), access))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<AccountSummaryDTO> getActiveAccountsForUserPaged(Integer userId, int page, int size, String sortBy, String direction) {
        Pageable pageable = PageRequest.of(page, size, createAccountSort(sortBy, direction));
        Page<AccountAccess> pageResult = accountAccessRepository.findActiveAccountsForUser(userId, pageable);

        List<AccountSummaryDTO> accounts = pageResult.getContent().stream()
                .map(access -> accountMapper.toAccountSummaryDTO(access.getAccount(), access))
                .toList();

        return new PageResponseDTO<>(
                accounts,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isFirst(),
                pageResult.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountCurrencySummaryDTO> getAccountCurrencySummary(Integer userId) {
        return accountAccessRepository.getCurrencySummaryForUser(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDetailsDTO getAccountDetails(Long accountId, Integer userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Contul cu id " + accountId + " nu a fost gasit"));

        AccountAccess access = accountAccessRepository.findByAccountAccountIdAndUserIdAndStatus(accountId, userId, "ACTIVE")
                .orElseThrow(() -> new AccessDeniedException("Nu aveti acces la acest cont"));

        return accountMapper.toAccountDetailsDTO(account, access);
    }

    @Override
    @Transactional
    public void closeAccount(Long accountId, Integer userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Contul cu id " + accountId + " nu a fost gasit"));

        AccountAccess access = accountAccessRepository.findByAccountAccountIdAndUserIdAndStatus(accountId, userId, "ACTIVE")
                .orElseThrow(() -> new AccessDeniedException("Nu aveti acces la acest cont"));

        if (!"OWNER".equalsIgnoreCase(access.getAccessRole())) {
            throw new AccessDeniedException("Doar un OWNER poate inchide acest cont!");
        }

        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new IllegalArgumentException("Doar conturile ACTIVE pot fi inchise!");
        }

        if (account.getBalance() != null && account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Soldul contului trebuie sa fie 0 pentru a putea fi inchis!");
        }

        account.setStatus("CLOSED");
        account.setUpdatedAt(new Date());
        accountRepository.save(account);
        log.info("Closed account {} by userId {}", accountId, userId);
    }

    @Override
    @Transactional
    public AccountResponseDTO createSharedAccount(SharedAccountRequest dto) {
        if (dto.getUsers() == null || dto.getUsers().isEmpty()) {
            throw new IllegalArgumentException("Trebuie specificat cel putin un utilizator.");
        }
        if (dto.getUsers().size() > 2) {
            throw new IllegalArgumentException("Un cont partajat poate avea maxim 2 utilizatori.");
        }

        boolean hasOwner = dto.getUsers().stream()
                .anyMatch(u -> "OWNER".equalsIgnoreCase(u.getRole()));
        if (!hasOwner) {
            throw new IllegalArgumentException("Contul trebuie sa aiba cel putin un utilizator cu rolul OWNER.");
        }

        Account account = new Account();
        account.setAlias(dto.getAlias());
        account.setCurrency(dto.getCurrency().toUpperCase());
        account.setBalance(BigDecimal.ZERO);
        account.setStatus("ACTIVE");
        account.setIban(generateUniqueIban());
        account.setCreatedAt(new Date());
        account.setUpdatedAt(new Date());
        Account savedAccount = accountRepository.save(account);

        for (UserRoleDTO userRole : dto.getUsers()) {
            UserLookupDTO userLookup = userClient.requireUserByEmail(userRole.getEmail());

            AccountAccess access = new AccountAccess();
            access.setAccount(savedAccount);
            access.setUserId(userLookup.getUserId());
            access.setAccessRole(userRole.getRole().toUpperCase());
            access.setStatus("ACTIVE");
            access.setCreatedAt(new Date());
            access.setUpdatedAt(new Date());
            accountAccessRepository.save(access);
        }

        log.info("Shared account {} created with {} users", savedAccount.getAccountId(), dto.getUsers().size());
        return accountMapper.toAccountResponseDTO(savedAccount);
    }

    @Override
    @Transactional
    public void revokeAccountAccess(Long accountId, String email) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Contul cu id " + accountId + " nu a fost gasit"));

        UserLookupDTO userLookup = userClient.requireUserByEmail(email);
        AccountAccess access = accountAccessRepository.findByAccountAccountIdAndUserIdAndStatus(accountId, userLookup.getUserId(), "ACTIVE")
                .orElseThrow(() -> new ResourceNotFoundException("Utilizatorul nu are acces activ la acest cont"));

        access.setStatus("INACTIVE");
        access.setUpdatedAt(new Date());
        accountAccessRepository.save(access);
        log.info("Revoked access for user {} on account {}", email, accountId);
    }

    private String generateUniqueIban() {
        String iban;
        do {
            String accountNumber = String.format("%016d", Math.abs(new Random().nextLong()) % 1_000_000_000_000_0000L);
            iban = "RO11BANK" + accountNumber;
        } while (accountRepository.existsByIban(iban));
        return iban;
    }

    private void validateCreateSingleAccountRequest(CreateSingleAccountRequestDTO dto) {
        if (dto.getAlias() == null || dto.getAlias().trim().isEmpty()) {
            throw new IllegalArgumentException("Account alias is required!");
        }
        if (dto.getCurrency() == null || dto.getCurrency().trim().isEmpty()) {
            throw new IllegalArgumentException("Account currency is required!");
        }
        String currency = dto.getCurrency().toUpperCase();
        if (!currency.equals("RON") && !currency.equals("EUR") && !currency.equals("USD")) {
            throw new IllegalArgumentException("Unsupported currency!");
        }
        if (dto.getInitialAmount() < 0) {
            throw new IllegalArgumentException("Initial amount must be positive or zero!");
        }
    }

    private Sort createAccountSort(String sortBy, String direction) {
        String sortProperty = switch (sortBy) {
            case "alias" -> "account.alias";
            case "balance" -> "account.balance";
            default -> throw new IllegalArgumentException("Invalid account sort field!");
        };

        Sort.Direction sortDirection;
        if ("asc".equalsIgnoreCase(direction)) {
            sortDirection = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(direction)) {
            sortDirection = Sort.Direction.DESC;
        } else {
            throw new IllegalArgumentException("Invalid sort direction!");
        }

        return Sort.by(sortDirection, sortProperty);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountInternalSummaryDTO getAccountInternalSummary(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Contul cu id " + accountId + " nu a fost gasit"));
        return new AccountInternalSummaryDTO(
                account.getAccountId(),
                account.getIban(),
                account.getAlias(),
                account.getCurrency(),
                account.getBalance(),
                account.getStatus()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AccountInternalSummaryDTO getAccountInternalSummaryByIban(String iban) {
        Account account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new ResourceNotFoundException("Contul cu IBAN-ul " + iban + " nu a fost gasit"));
        return new AccountInternalSummaryDTO(
                account.getAccountId(),
                account.getIban(),
                account.getAlias(),
                account.getCurrency(),
                account.getBalance(),
                account.getStatus()
        );
    }

    @Override
    @Transactional
    public AccountInternalSummaryDTO debitAccount(Long accountId, java.math.BigDecimal amount, String operationId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Contul cu id " + accountId + " nu a fost gasit"));

        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new IllegalArgumentException("Contul sursa nu este activ!");
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Fonduri insuficiente!");
        }

        account.setBalance(account.getBalance().subtract(amount));
        account.setUpdatedAt(new Date());
        Account saved = accountRepository.save(account);
        log.info("Debited {} {} from account {} (operationId: {})", amount, saved.getCurrency(), accountId, operationId);

        return new AccountInternalSummaryDTO(
                saved.getAccountId(),
                saved.getIban(),
                saved.getAlias(),
                saved.getCurrency(),
                saved.getBalance(),
                saved.getStatus()
        );
    }

    @Override
    @Transactional
    public AccountInternalSummaryDTO creditAccount(Long accountId, java.math.BigDecimal amount, String operationId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Contul cu id " + accountId + " nu a fost gasit"));

        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new IllegalArgumentException("Contul destinatie nu este activ!");
        }

        account.setBalance(account.getBalance().add(amount));
        account.setUpdatedAt(new Date());
        Account saved = accountRepository.save(account);
        log.info("Credited {} {} to account {} (operationId: {})", amount, saved.getCurrency(), accountId, operationId);

        return new AccountInternalSummaryDTO(
                saved.getAccountId(),
                saved.getIban(),
                saved.getAlias(),
                saved.getCurrency(),
                saved.getBalance(),
                saved.getStatus()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkUserAccountAccess(Long accountId, Integer userId, String requiredRole) {
        java.util.Optional<AccountAccess> accessOpt = accountAccessRepository.findByAccountAccountIdAndUserIdAndStatus(accountId, userId, "ACTIVE");
        if (accessOpt.isEmpty()) {
            return false;
        }
        if (requiredRole == null || requiredRole.isBlank()) {
            return true;
        }
        String userRole = accessOpt.get().getAccessRole();
        if ("OWNER".equalsIgnoreCase(userRole)) {
            return true;
        }
        if ("CO_OWNER".equalsIgnoreCase(userRole)) {
            return !"OWNER".equalsIgnoreCase(requiredRole);
        }
        if ("VIEWER".equalsIgnoreCase(userRole)) {
            return "VIEWER".equalsIgnoreCase(requiredRole) || "READ".equalsIgnoreCase(requiredRole);
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountInternalSummaryDTO> getActiveAccountInternalSummariesForUser(Integer userId) {
        List<AccountAccess> accesses = accountAccessRepository.findByUserIdAndStatus(userId, "ACTIVE");
        return accesses.stream()
                .filter(access -> "ACTIVE".equalsIgnoreCase(access.getAccount().getStatus()))
                .map(access -> new AccountInternalSummaryDTO(
                        access.getAccount().getAccountId(),
                        access.getAccount().getIban(),
                        access.getAccount().getAlias(),
                        access.getAccount().getCurrency(),
                        access.getAccount().getBalance(),
                        access.getAccount().getStatus()
                ))
                .toList();
    }
}
