package com.example.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountSummaryDTO {
    private Long accountId;
    private String alias;
    private String iban;
    private String currency;
    private double balance;
    private String accountRole;
}
