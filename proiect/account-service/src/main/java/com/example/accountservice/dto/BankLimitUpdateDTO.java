package com.example.accountservice.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BankLimitUpdateDTO {
    @DecimalMin(value = "0.01", message = "The maximum limit per transaction must be positive.")
    private BigDecimal maxAmountPerTransactionRon;

    @DecimalMin(value = "0.01", message = "The daily limit must be positive.")
    private BigDecimal maxDailyAmountRon;

    @DecimalMin(value = "0.01", message = "The maximum number of transactions must be positive.")
    private BigDecimal maxDailyTransactionsCount;
}
