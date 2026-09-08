package com.example.accountservice.mappers;

import com.example.accountservice.domain.Account;
import com.example.accountservice.domain.AccountAccess;
import com.example.accountservice.dto.AccountDetailsDTO;
import com.example.accountservice.dto.AccountResponseDTO;
import com.example.accountservice.dto.AccountSummaryDTO;
import com.example.accountservice.dto.CreateSingleAccountRequestDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AccountMapper {

    public Account toAccount(CreateSingleAccountRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        Account account = new Account();
        account.setAlias(dto.getAlias());
        account.setCurrency(dto.getCurrency() != null ? dto.getCurrency().toUpperCase() : null);
        account.setBalance(BigDecimal.valueOf(dto.getInitialAmount() != null ? dto.getInitialAmount() : 0.0));
        account.setStatus("ACTIVE");
        return account;
    }

    public AccountResponseDTO toAccountResponseDTO(Account account) {
        if (account == null) {
            return null;
        }
        AccountResponseDTO dto = new AccountResponseDTO();
        dto.setAccountId(account.getAccountId());
        dto.setAlias(account.getAlias());
        dto.setIban(account.getIban());
        dto.setCurrency(account.getCurrency());
        dto.setBalance(account.getBalance() != null ? account.getBalance().doubleValue() : 0.0);
        dto.setStatus(account.getStatus());
        return dto;
    }

    public AccountSummaryDTO toAccountSummaryDTO(Account account, AccountAccess access) {
        if (account == null) {
            return null;
        }
        AccountSummaryDTO dto = new AccountSummaryDTO();
        dto.setAccountId(account.getAccountId());
        dto.setAlias(account.getAlias());
        dto.setIban(account.getIban());
        dto.setCurrency(account.getCurrency());
        dto.setBalance(account.getBalance() != null ? account.getBalance().doubleValue() : 0.0);
        dto.setAccountRole(access != null ? access.getAccessRole() : null);
        return dto;
    }

    public AccountDetailsDTO toAccountDetailsDTO(Account account, AccountAccess access) {
        if (account == null) {
            return null;
        }
        AccountDetailsDTO dto = new AccountDetailsDTO();
        dto.setAccountId(account.getAccountId());
        dto.setAlias(account.getAlias());
        dto.setIban(account.getIban());
        dto.setCurrency(account.getCurrency());
        dto.setBalance(account.getBalance() != null ? account.getBalance().doubleValue() : 0.0);
        dto.setStatus(account.getStatus());

        String role = access != null ? access.getAccessRole() : null;
        dto.setAccountRole(role);
        dto.setCanInitiateTransactions("OWNER".equalsIgnoreCase(role) || "CO_OWNER".equalsIgnoreCase(role));
        return dto;
    }
}
