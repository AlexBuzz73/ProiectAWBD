package com.example.accountservice.services;

import com.example.accountservice.dto.BankLimitRequestDTO;
import com.example.accountservice.dto.BankLimitResponseDTO;
import com.example.accountservice.dto.UserLimitRequestDTO;
import com.example.accountservice.dto.UserLimitResponseDTO;

public interface LimitService {

    BankLimitResponseDTO getBankLimits();

    BankLimitResponseDTO updateBankLimits(BankLimitRequestDTO dto);

    void deleteBankLimits(Integer bankLimitId);

    UserLimitResponseDTO getUserLimits(Integer userId);

    UserLimitResponseDTO updateUserLimits(Integer userId, UserLimitRequestDTO dto);

    void deleteUserLimits(Integer userId);
}
