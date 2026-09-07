package com.example.accountservice.services;

import com.example.accountservice.dto.*;

import java.util.List;

public interface AccountService {

    AccountResponseDTO createSingleAccount(CreateSingleAccountRequestDTO dto, Integer userId);

    List<AccountSummaryDTO> getActiveAccountsForUser(Integer userId);

    PageResponseDTO<AccountSummaryDTO> getActiveAccountsForUserPaged(Integer userId, int page, int size, String sortBy, String direction);

    List<AccountCurrencySummaryDTO> getAccountCurrencySummary(Integer userId);

    AccountDetailsDTO getAccountDetails(Long accountId, Integer userId);

    void closeAccount(Long accountId, Integer userId);

    AccountResponseDTO createSharedAccount(SharedAccountRequest dto);

    void revokeAccountAccess(Long accountId, String email);

    AccountInternalSummaryDTO getAccountInternalSummary(Long accountId);

    AccountInternalSummaryDTO getAccountInternalSummaryByIban(String iban);

    AccountInternalSummaryDTO debitAccount(Long accountId, java.math.BigDecimal amount, String operationId);

    AccountInternalSummaryDTO creditAccount(Long accountId, java.math.BigDecimal amount, String operationId);

    boolean checkUserAccountAccess(Long accountId, Integer userId, String requiredRole);
}
