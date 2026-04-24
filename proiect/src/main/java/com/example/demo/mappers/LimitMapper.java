package com.example.demo.mappers;

import com.example.demo.domain.BankLimit;
import com.example.demo.domain.UserLimit;
import com.example.demo.dto.BankLimitRequestDTO;
import com.example.demo.dto.BankLimitResponseDTO;
import com.example.demo.dto.UserLimitRequestDTO;
import com.example.demo.dto.UserLimitResponseDTO;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class LimitMapper {

    public BankLimit toBankLimit(BankLimitRequestDTO bankLimitRequestDTO) {
        BankLimit bankLimit = new BankLimit();
        bankLimit.setMaxAmountPerTransactionRon(bankLimitRequestDTO.getMaxAmountPerTransactionRon());
        bankLimit.setMaxDailyAmountRon(bankLimitRequestDTO.getMaxDailyAmountRon());
        bankLimit.setMaxDailyTransactionsCount(bankLimitRequestDTO.getMaxDailyTransactionsCount());
        bankLimit.setStatus("ACTIVE");
        bankLimit.setCreatedAt(new Date());
        bankLimit.setUpdatedAt(new Date());

        return bankLimit;
    }

    public UserLimit toUserLimit(UserLimitRequestDTO userLimitRequestDTO) {
        UserLimit userLimit = new UserLimit();
        userLimit.setMaxAmountPerTransactionRon(userLimitRequestDTO.getMaxAmountPerTransactionRon());
        userLimit.setMaxDailyAmountRon(userLimitRequestDTO.getMaxDailyAmountRon());
        userLimit.setMaxDailyTransactionsCount(userLimitRequestDTO.getMaxDailyTransactionsCount());
        userLimit.setStatus("ACTIVE");
        userLimit.setCreatedAt(new Date());
        userLimit.setUpdatedAt(new Date());

        return userLimit;
    }

    public UserLimitResponseDTO toUserLimitResponseDTO(UserLimit userLimit) {
        return new UserLimitResponseDTO(userLimit.getUserLimitId(), userLimit.getMaxAmountPerTransactionRon(), userLimit.getMaxDailyAmountRon(), userLimit.getMaxAmountPerTransactionRon(), userLimit.getStatus());
    }

    public BankLimitResponseDTO bankLimitResponseDTO(BankLimit bankLimit) {
        return new BankLimitResponseDTO(bankLimit.getBankLimitId(), bankLimit.getMaxAmountPerTransactionRon(), bankLimit.getMaxDailyAmountRon(), bankLimit.getMaxDailyTransactionsCount(), bankLimit.getStatus());
    }
}
