package com.example.demo.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BankLimitUpdateDTO {
    private BigDecimal maxAmountPerTransactionRon;
    private BigDecimal maxDailyAmountRon;
    private Integer maxDailyTransactionsCount;
}