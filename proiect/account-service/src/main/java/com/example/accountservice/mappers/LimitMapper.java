package com.example.accountservice.mappers;

import com.example.accountservice.domain.BankLimit;
import com.example.accountservice.domain.UserLimit;
import com.example.accountservice.dto.BankLimitResponseDTO;
import com.example.accountservice.dto.UserLimitResponseDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class LimitMapper {

    public BankLimitResponseDTO toBankLimitResponseDTO(BankLimit bankLimit) {
        if (bankLimit == null) {
            return null;
        }
        BankLimitResponseDTO dto = new BankLimitResponseDTO();
        dto.setBankLimitId(bankLimit.getBankLimitId());
        dto.setMaxAmountPerTransactionRon(bankLimit.getMaxAmountPerTransactionRon());
        dto.setMaxDailyAmountRon(bankLimit.getMaxDailyAmountRon());
        dto.setMaxDailyTransactionsCount(bankLimit.getMaxDailyTransactionsCount());
        dto.setStatus(bankLimit.getStatus());
        return dto;
    }

    public UserLimitResponseDTO toUserLimitResponseDTO(UserLimit userLimit) {
        if (userLimit == null) {
            return null;
        }
        UserLimitResponseDTO dto = new UserLimitResponseDTO();
        dto.setUserLimitId(userLimit.getUserLimitId());
        dto.setMaxAmountPerTransactionRon(userLimit.getMaxAmountPerTransactionRon());
        dto.setMaxDailyAmountRon(userLimit.getMaxDailyAmountRon());
        dto.setMaxDailyTransactionsCount(userLimit.getMaxDailyTransactionsCount());
        dto.setStatus(userLimit.getStatus());
        return dto;
    }

    public UserLimitResponseDTO toEmptyUserLimitResponseDTO() {
        UserLimitResponseDTO dto = new UserLimitResponseDTO();
        dto.setUserLimitId(0);
        dto.setMaxAmountPerTransactionRon(BigDecimal.ZERO);
        dto.setMaxDailyAmountRon(BigDecimal.ZERO);
        dto.setMaxDailyTransactionsCount(BigDecimal.ZERO);
        dto.setStatus("INACTIVE");
        return dto;
    }
}
