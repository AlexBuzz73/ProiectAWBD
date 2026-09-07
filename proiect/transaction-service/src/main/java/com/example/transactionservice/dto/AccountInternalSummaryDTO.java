package com.example.transactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountInternalSummaryDTO {
    private Long accountId;
    private String iban;
    private String alias;
    private String currency;
    private BigDecimal balance;
    private String status;
}
