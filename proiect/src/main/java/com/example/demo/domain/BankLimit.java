package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "bank_limits")
public class BankLimit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int bankLimitId;
    private int maxAmountPerTransactionRon;
    private int maxDailyAmountRon;
    private int maxDailyTransactionsCount;
    private String status;
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    public BankLimit() {
    }
}