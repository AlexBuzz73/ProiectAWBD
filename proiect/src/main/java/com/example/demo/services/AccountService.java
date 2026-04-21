package com.example.demo.services;

import com.example.demo.dto.AccountDetailsDTO;
import com.example.demo.dto.AccountResponseDTO;
import com.example.demo.dto.AccountSummaryDTO;
import com.example.demo.dto.CreateSingleAccountRequestDTO;

import java.util.List;

public interface AccountService {

    AccountResponseDTO createSingleAccount(CreateSingleAccountRequestDTO accountDTO, int userId);
    List<AccountSummaryDTO> getActiveAccountsForUser(int userId);
    AccountDetailsDTO getAccountDetails(Long accountId, int userId);
    void closeAccount(Long accountId, int userId);
}
