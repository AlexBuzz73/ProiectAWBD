package com.example.demo.services;

import com.example.demo.domain.Account;
import com.example.demo.dto.BankLimitUpdateDTO;
import com.example.demo.dto.SharedAccountRequest;

public interface AdminService {

    Account createSharedAccount(SharedAccountRequest dto);
    void updateBankLimits(BankLimitUpdateDTO dto);
    void revokeAccountAccess(Long accountId, String email);
}