package com.example.demo.services.impl;

import com.example.demo.domain.BankLimit;
import com.example.demo.domain.User;
import com.example.demo.domain.UserLimit;
import com.example.demo.dto.BankLimitRequestDTO;
import com.example.demo.dto.BankLimitResponseDTO;
import com.example.demo.dto.UserLimitRequestDTO;
import com.example.demo.dto.UserLimitResponseDTO;
import com.example.demo.mappers.LimitMapper;
import com.example.demo.repositories.BankLimitRepository;
import com.example.demo.repositories.UserLimitRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.LimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LimitServiceImpl implements LimitService {

    private final BankLimitRepository bankLimitRepository;
    private final UserLimitRepository userLimitRepository;
    private final LimitMapper limitMapper;
    private final UserRepository userRepository;


    @Override
    public BankLimitResponseDTO getBankLimits() {
        BankLimit bankLimit = bankLimitRepository.findByStatus("ACTIVE").orElseThrow( () -> new IllegalArgumentException("Active bank limits not found!"));

        return limitMapper.toBankLimitResponseDTO(bankLimit);
    }

    @Override
    public BankLimitResponseDTO updateBankLimits(BankLimitRequestDTO bankLimitRequestDTO) {
        BankLimit bankLimit = bankLimitRepository.findByStatus("ACTIVE").orElseThrow( () -> new IllegalArgumentException("Active bank limits not found!"));
        bankLimit.setMaxAmountPerTransactionRon(bankLimitRequestDTO.getMaxAmountPerTransactionRon());
        bankLimit.setMaxDailyAmountRon(bankLimitRequestDTO.getMaxDailyAmountRon());
        bankLimit.setMaxDailyTransactionsCount(bankLimitRequestDTO.getMaxDailyTransactionsCount());
        bankLimit.setUpdatedAt(new Date());

        BankLimit savedBankLimits = bankLimitRepository.save(bankLimit);

        return limitMapper.toBankLimitResponseDTO(savedBankLimits);
    }

    @Override
    public UserLimitResponseDTO getUserLimits(Integer userId) {

        Optional<UserLimit> optionalUserLimits = userLimitRepository.findByUserUserIdAndStatus(userId, "ACTIVE");

        if(optionalUserLimits.isPresent()) {
            return limitMapper.toUserLimitResponseDTO(optionalUserLimits.get());
        }

        return limitMapper.toEmptyUserLimitResponseDTO();
    }

    @Override
    public UserLimitResponseDTO updateUserLimits(Integer userId, UserLimitRequestDTO userLimitRequestDTO) {
        User user = userRepository.findById(userId).orElseThrow( () -> new IllegalArgumentException("User not found!"));
        validateUserActive(user);

        Optional<UserLimit> optionalUserLimit = userLimitRepository.findByUserUserIdAndStatus(userId, "ACTIVE");

        UserLimit userLimit;

        if(optionalUserLimit.isPresent()) {
            userLimit = optionalUserLimit.get();
        } else {
            userLimit = new UserLimit();
            userLimit.setUser(user);
            userLimit.setStatus("ACTIVE");
            userLimit.setCreatedAt(new Date());
        }

        userLimit.setMaxAmountPerTransactionRon(userLimitRequestDTO.getMaxAmountPerTransactionRon());
        userLimit.setMaxDailyAmountRon(userLimitRequestDTO.getMaxDailyAmountRon());
        userLimit.setMaxDailyTransactionsCount(userLimitRequestDTO.getMaxDailyTransactionsCount());
        userLimit.setUpdatedAt(new Date());
        UserLimit savedUserLimit = userLimitRepository.save(userLimit);

        return limitMapper.toUserLimitResponseDTO(savedUserLimit);

    }

    @Override
    public void validateRequiredFields(UserLimitRequestDTO userLimitRequestDTO, BankLimitRequestDTO bankLimitRequestDTO) {
        if(userLimitRequestDTO.getMaxAmountPerTransactionRon() == null) {
            throw new IllegalArgumentException("User: Max Amount Per Transaction in Ron is required!");
        }
        if(userLimitRequestDTO.getMaxDailyAmountRon() == null) {
            throw new IllegalArgumentException("User: Max Daily Amount in Ron is required!");
        }
        if(userLimitRequestDTO.getMaxDailyTransactionsCount() == null) {
            throw new IllegalArgumentException("User: Max Daily Transactions is required!");
        }

        if(bankLimitRequestDTO.getMaxAmountPerTransactionRon() == null) {
            throw new IllegalArgumentException("Bank: Max Amount Per Transaction in Ron is required!");
        }
        if(bankLimitRequestDTO.getMaxDailyAmountRon() == null) {
            throw new IllegalArgumentException("Bank: Max Daily Amount in Ron is required!");
        }
        if(bankLimitRequestDTO.getMaxDailyTransactionsCount() == null) {
            throw new IllegalArgumentException("Bank: Max Daily Transactions is required!");
        }
    }


    @Override
    public void validatePositiveValues(UserLimitRequestDTO userLimitRequestDTO, BankLimitRequestDTO bankLimitRequestDTO) {
        BigDecimal MaxAmountPerTransactionRon = userLimitRequestDTO.getMaxAmountPerTransactionRon();
        if( MaxAmountPerTransactionRon.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("User: Max Amount Per Transaction in Ron must be greater than zero!");
        }

        BigDecimal MaxDailyAmountRon = userLimitRequestDTO.getMaxDailyAmountRon();
        if(MaxDailyAmountRon.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("User: Max Daily Amount in Ron must be greater than zero!");
        }

        BigDecimal MaxDailyTransactionsCount = userLimitRequestDTO.getMaxDailyTransactionsCount();
        if(MaxDailyTransactionsCount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("User: Max Daily Transactions must be greater than zero!");
        }

        BigDecimal bankMaxAmountPerTransactionRon = bankLimitRequestDTO.getMaxAmountPerTransactionRon();
        if( bankMaxAmountPerTransactionRon.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bank: Max Amount Per Transaction in Ron must be greater than zero!");
        }

        BigDecimal bankMaxDailyAmountRon = bankLimitRequestDTO.getMaxDailyAmountRon();
        if(bankMaxDailyAmountRon.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bank: Max Daily Amount in Ron must be greater than zero!");
        }

        BigDecimal bankMaxDailyTransactionsCount = bankLimitRequestDTO.getMaxDailyTransactionsCount();
        if(bankMaxDailyTransactionsCount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bank: Max Daily Transactions must be greater than zero!");
        }
    }

    @Override
    public void validateUserLimitsAgainstBankLimits(UserLimitRequestDTO userLimitRequestDTO, BankLimitRequestDTO bankLimitRequestDTO) {

        BigDecimal bankMaxAmountPerTransactionRon = bankLimitRequestDTO.getMaxAmountPerTransactionRon();
        BigDecimal userMaxAmountPerTransactionRon = userLimitRequestDTO.getMaxAmountPerTransactionRon();
        if( userMaxAmountPerTransactionRon.compareTo(bankMaxAmountPerTransactionRon) <= 0) {
            throw new IllegalArgumentException("The user Max Amount Per Transaction in Ron is greater than the bank Max Amount Per Transaction in Ron!");
        }

        BigDecimal bankMaxDailyAmountRon = bankLimitRequestDTO.getMaxDailyAmountRon();
        BigDecimal userMaxDailyAmountRon = userLimitRequestDTO.getMaxDailyAmountRon();
        if( userMaxDailyAmountRon.compareTo(bankMaxDailyAmountRon) <= 0) {
            throw new IllegalArgumentException("The user Max Daily Amount in Ron is greater than the bank Max Daily Amount in Ron!");
        }

        BigDecimal bankMaxDailyTransactionsCount = bankLimitRequestDTO.getMaxDailyTransactionsCount();
        BigDecimal userMaxDailyTransactionsCount = userLimitRequestDTO.getMaxDailyTransactionsCount();
        if( userMaxDailyTransactionsCount.compareTo(bankMaxDailyTransactionsCount) <= 0) {
            throw new IllegalArgumentException("The user Max Daily Transactions is greater than the bank Max Daily Transactions!");
        }
    }

    @Override
    public void validateUserActive(User user) {
        if(user.getStatus() == null) {
            throw new IllegalArgumentException("User: User not found!");
        }
        if(!user.getStatus().equals("ACTIVE")) {
            throw new IllegalArgumentException("User: User is not active!");
        }
    }

    @Override
    public void validateBankLimitsActive(BankLimit bankLimit) {
        if(!bankLimit.getStatus().equals("ACTIVE")) {
            throw new IllegalArgumentException("Bank: Bank Limit is not active!");
        }

        if (bankLimit.getStatus() == null) {
            throw new IllegalArgumentException("Bank: Bank Limit is null!");
        }
    }
}
