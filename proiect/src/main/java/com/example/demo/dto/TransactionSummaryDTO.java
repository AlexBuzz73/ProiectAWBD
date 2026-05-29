package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionSummaryDTO {
    private int transactionId;
    private String transactionType;
    private double amount;
    private String currency;
    private String description;
    private String status;
    private Date createdAt;
    private Long sourceAccountId;
    private String sourceAccountAlias;
    private String sourceAccountIban;
    private Long destinationAccountId;
    private String destinationAccountAlias;
    private String destinationAccountIban; // pentru INTERNAL
    private String destinationIban; // pentru EXTERNAL
    private Integer categoryId;
    private String categoryName;
}
