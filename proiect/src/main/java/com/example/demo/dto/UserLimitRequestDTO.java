package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserLimitRequestDTO {

    private BigDecimal maxAmountPerTransactionRon;
    private BigDecimal maxDailyAmountRon;
    private BigDecimal maxDailyTransactionsCount;



}
