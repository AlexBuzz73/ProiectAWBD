package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BankLimitRequestDTO {

    @NotNull(message = "The maximum limit per transaction is mandatory.")
    @DecimalMin(value = "0.01", message = "The maximum limit per transaction must be positive.")
    private BigDecimal maxAmountPerTransactionRon;

    @NotNull(message = "The daily limit is mandatory.")
    @DecimalMin(value = "0.01", message = "The daily limit must be positive.")
    private BigDecimal maxDailyAmountRon;

    @NotNull(message = "The maximum number of transactions is mandatory.")
    @DecimalMin(value = "0.01", message = "The maximum number of transactions must be positive.")
    private BigDecimal maxDailyTransactionsCount;
}
