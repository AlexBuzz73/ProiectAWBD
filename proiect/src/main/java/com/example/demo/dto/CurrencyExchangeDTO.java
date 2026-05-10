package com.example.demo.dto;

import lombok.Data;

@Data
public class CurrencyExchangeDTO {
    private Integer sourceAccountId;
    private Integer destinationAccountId;
    private double amount;
    private Integer categoryId;
    private String description;
    private String password;
}