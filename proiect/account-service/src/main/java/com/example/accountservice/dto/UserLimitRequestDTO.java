package com.example.accountservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserLimitRequestDTO {

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
