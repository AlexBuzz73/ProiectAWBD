package com.example.transactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionSummaryDTO {
    private Long transactionId;
    private String transactionType;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String status;
    private Date createdAt;
    private Long sourceAccountId;
    private String sourceAccountAlias;
    private String sourceAccountIban;
    private Long destinationAccountId;
    private String destinationAccountAlias;
    private String destinationAccountIban;
    private String destinationIban;
    private Integer categoryId;
    private String categoryName;
}
