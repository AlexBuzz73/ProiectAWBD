package com.example.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BankLimitResponseDTO {
    private int bankLimitId;
    private BigDecimal maxAmountPerTransactionRon;
    private BigDecimal maxDailyAmountRon;
    private BigDecimal maxDailyTransactionsCount;
    private String status;
}
