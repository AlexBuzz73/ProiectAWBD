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
public class AccountCurrencySummaryDTO {
    private String currency;
    private Double totalBalance;
    private Long accountCount;

    public AccountCurrencySummaryDTO(String currency, Long accountCount, BigDecimal totalBalance) {
        this.currency = currency;
        this.accountCount = accountCount;
        this.totalBalance = totalBalance != null ? totalBalance.doubleValue() : 0.0;
    }
}
