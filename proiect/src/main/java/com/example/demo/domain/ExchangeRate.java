package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "exchange_rates", uniqueConstraints = @UniqueConstraint(columnNames = {"currency_from", "currency_to", "rate_date"}))
public class ExchangeRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int exchangeRateId;
    private String currencyFrom;
    private String currencyTo;
    private double rate;
    @Temporal(TemporalType.DATE)
    private Date rateDate;
    private String source;
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    public ExchangeRate() {
    }

    @OneToMany(mappedBy = "exchangeRate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions;
}