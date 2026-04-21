package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponseDTO {
    private Long accountId;
    private String alias;
    private String iban;
    private String currency;
    private double balance;
    private String status;
}
