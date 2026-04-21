package com.example.demo.mappers;

import com.example.demo.domain.Account;
import com.example.demo.domain.AccountAccess;
import com.example.demo.dto.AccountDetailsDTO;
import com.example.demo.dto.AccountResponseDTO;
import com.example.demo.dto.AccountSummaryDTO;
import com.example.demo.dto.CreateSingleAccountRequestDTO;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class AccountMapper {
    public Account toAccount(CreateSingleAccountRequestDTO dto) {
        Account account = new Account();
        account.setAlias(dto.getAlias());
        account.setCurrency(dto.getCurrency());
        account.setBalance(0.0);
        account.setStatus("ACTIVE");
        account.setCreatedAt(new Date());
        account.setUpdatedAt(new Date());

        return account;
    }

    public AccountResponseDTO toAccountResponseDTO(Account account) {
        return new AccountResponseDTO(
                account.getAccountId(),
                account.getAlias(),
                account.getIban(),
                account.getCurrency(),
                account.getBalance(),
                account.getStatus()
        );
    }

    public AccountSummaryDTO toAccountSummaryDTO(Account account, AccountAccess accountAccess) {
        return new AccountSummaryDTO(
                account.getAccountId(),
                account.getAlias(),
                account.getIban(),
                account.getCurrency(),
                account.getBalance(),
                accountAccess.getAccessRole()
        );
    }

    public AccountDetailsDTO toAccountDetailsDTO(Account account, AccountAccess accountAccess) {
        boolean canInitiateTransactions = "OWNER".equals(accountAccess.getAccessRole()) || "CO_OWNER".equals(accountAccess.getAccessRole());

        return new AccountDetailsDTO(
                account.getAccountId(),
                account.getAlias(),
                account.getIban(),
                account.getCurrency(),
                account.getBalance(),
                account.getStatus(),
                accountAccess.getAccessRole(),
                canInitiateTransactions
        );
    }
}
