package com.example.demo.services;

import com.example.demo.domain.BankLimit;
import com.example.demo.domain.User;
import com.example.demo.dto.BankLimitRequestDTO;
import com.example.demo.dto.UserLimitRequestDTO;
import com.example.demo.dto.UserLimitResponseDTO;

public interface LimitService {


    void validateRequiredFields(UserLimitRequestDTO userLimitRequestDTO, BankLimitRequestDTO bankLimitRequestDTO);
    void validatePositiveValues(UserLimitRequestDTO userLimitRequestDTO, BankLimitRequestDTO bankLimitRequestDTO);
    void validateUserLimitsAgainstBankLimits(UserLimitRequestDTO userLimitRequestDTO, BankLimitRequestDTO bankLimitRequestDTO);
    void validateUserActive(User user);
    void validateBankLimitsActive(BankLimit bankLimit);
}
