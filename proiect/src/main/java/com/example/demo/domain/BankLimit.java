package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@Table(name = "bank_limits")
public class BankLimit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int bankLimitId;
    private BigDecimal maxAmountPerTransactionRon;
    private BigDecimal maxDailyAmountRon;
    private BigDecimal maxDailyTransactionsCount;
    private String status;
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    public BankLimit() {
    }
}