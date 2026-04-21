package com.example.demo.services.impl;

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
import com.example.demo.services.AccountService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountAccessRepository accountAccessRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;

    @Override
    @Transactional
    public AccountResponseDTO createSingleAccount(CreateSingleAccountRequestDTO accountDTO, int userId) {
        validateCreateSingleAccountRequest(accountDTO);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found!"));

        if(!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalArgumentException("User is not active!");
        }

        Account account = accountMapper.toAccount(accountDTO);
        account.setIban(generateUniqueIban());
        Account savedAccount = accountRepository.save(account);

        AccountAccess accountAccess = new AccountAccess();
        accountAccess.setAccount(savedAccount);
        accountAccess.setUser(user);
        accountAccess.setAccessRole("OWNER");
        accountAccess.setStatus("ACTIVE");
        accountAccess.setCreatedAt(new Date());
        accountAccess.setUpdatedAt(new Date());
        accountAccessRepository.save(accountAccess);

        Transaction transaction = new Transaction();
        transaction.setTransactionType("TOP_UP");
        transaction.setDestinationAccount(savedAccount);
        transaction.setAmount(accountDTO.getInitialAmount());
        transaction.setCurrency(savedAccount.getCurrency());
        transaction.setStatus("EXECUTED");
        transaction.setCreatedAt(new Date());
        transaction.setUpdatedAt(new Date());
        transactionRepository.save(transaction);

        savedAccount.setBalance(accountDTO.getInitialAmount());
        savedAccount.setUpdatedAt(new Date());
        accountRepository.save(savedAccount);

        return accountMapper.toAccountResponseDTO(savedAccount);
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
        validateAlias(dto.getAlias());
        validateCurrency(dto.getCurrency());
        validateInitialAmount(dto.getInitialAmount());
        validateExternalIban(dto.getExternalIban());
    }

    private void validateAlias(String alias) {
        if (alias == null || alias.trim().isEmpty()) {
            throw new IllegalArgumentException("Account alias is required!");
        }
    }

    private void validateCurrency(String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Account currency is required!");
        }

        if(!currency.equals("RON") && !currency.equals("EUR") && !currency.equals("USD")) {
            throw new IllegalArgumentException("Unsupported currency!");
        }
    }

    private void validateInitialAmount(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Initial amount must be positive!");
        }
    }

    private void validateExternalIban(String iban) {
        if (iban == null || iban.trim().isEmpty()) {
            throw new IllegalArgumentException("External IBAN is required!");
        }
    }

    @Override
    public List<AccountSummaryDTO> getActiveAccountsForUser(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found!"));

        if(!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalArgumentException("User is not active!");
        }

        List<AccountAccess> accountAccessList = accountAccessRepository.findByUserUserIdAndStatus(userId, "ACTIVE");

        return accountAccessList.stream().filter(access -> "ACTIVE".equals(access.getAccount().getStatus()))
                .map(access -> accountMapper.toAccountSummaryDTO(access.getAccount(), access)).toList();
    }

    @Override
    public AccountDetailsDTO getAccountDetails(Long accountId, int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found!"));

        if(!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalArgumentException("User is not active!");
        }

        AccountAccess accountAccess = accountAccessRepository.findByAccountAccountIdAndUserUserIdAndStatus(accountId, userId, "ACTIVE")
                .orElseThrow(() -> new IllegalArgumentException("You don't have access to this account!"));

        return accountMapper.toAccountDetailsDTO(accountAccess.getAccount(), accountAccess);
    }

    @Override
    public void closeAccount(Long accountId, int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found!"));

        if(!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalArgumentException("User is not active!");
        }

        AccountAccess accountAccess = accountAccessRepository.findByAccountAccountIdAndUserUserIdAndStatus(accountId, userId, "ACTIVE")
                .orElseThrow(() -> new IllegalArgumentException("You don't have access to this account!"));

        if(!"OWNER".equals(accountAccess.getAccessRole())) {
            throw new IllegalArgumentException("Only the OWNER can close this account!");
        }

        Account account = accountAccess.getAccount();

        if(!"ACTIVE".equals(account.getStatus())) {
            throw new IllegalArgumentException("Only ACTIVE accounts can be closed!");
        }

        if(account.getBalance() != 0) {
            throw new IllegalArgumentException("Account balance must be 0 to close the account!");
        }

        account.setStatus("CLOSED");
        account.setUpdatedAt(new Date());

        accountRepository.save(account);
    }
}
