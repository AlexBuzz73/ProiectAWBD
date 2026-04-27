package com.example.demo.services;

import com.example.demo.domain.BankLimit;
import com.example.demo.domain.User;
import com.example.demo.dto.BankLimitRequestDTO;
import com.example.demo.dto.BankLimitResponseDTO;
import com.example.demo.dto.UserLimitRequestDTO;
import com.example.demo.dto.UserLimitResponseDTO;

import java.math.BigDecimal;
import java.util.List;

public interface LimitService {

    BankLimitResponseDTO getBankLimits();
    BankLimitResponseDTO updateBankLimits(BankLimitRequestDTO bankLimitRequestDTO);
    UserLimitResponseDTO getUserLimits(Integer userId);
    UserLimitResponseDTO updateUserLimits(Integer userId, UserLimitRequestDTO userLimitRequestDTO);
    void validateRequiredFields(UserLimitRequestDTO userLimitRequestDTO, BankLimitRequestDTO bankLimitRequestDTO);
    void validatePositiveValues(UserLimitRequestDTO userLimitRequestDTO, BankLimitRequestDTO bankLimitRequestDTO);
    void validateUserLimitsAgainstBankLimits(UserLimitRequestDTO userLimitRequestDTO, BankLimitRequestDTO bankLimitRequestDTO);
    void validateUserActive(User user);
    void validateBankLimitsActive(BankLimit bankLimit);
}
