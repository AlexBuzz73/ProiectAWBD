package com.example.demo.services;

import com.example.demo.dto.*;

import java.util.List;

public interface AccountService {

    AccountResponseDTO createSingleAccount(CreateSingleAccountRequestDTO accountDTO, int userId);
    List<AccountSummaryDTO> getActiveAccountsForUser(int userId);
    AccountDetailsDTO getAccountDetails(Long accountId, int userId);
    void closeAccount(Long accountId, int userId);
    PageResponseDTO<AccountSummaryDTO> getActiveAccountsForUserPaged(int userId, int page, int size, String sortBy, String direction);
    List<AccountCurrencySummaryDTO> getAccountCurrencySummary(int userId);
}
