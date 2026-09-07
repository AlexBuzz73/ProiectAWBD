package com.example.transactionservice.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "exchange_rates", uniqueConstraints = @UniqueConstraint(columnNames = {"currency_from", "currency_to", "rate_date"}))
public class ExchangeRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exchange_rate_id")
    private Integer exchangeRateId;

    @Column(name = "currency_from", length = 3)
    private String currencyFrom;

    @Column(name = "currency_to", length = 3)
    private String currencyTo;

    @Column(name = "rate", precision = 19, scale = 6, nullable = false)
    private BigDecimal rate;

    @Temporal(TemporalType.DATE)
    @Column(name = "rate_date")
    private Date rateDate;

    @Column(name = "source", length = 50)
    private String source;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt;

    @OneToMany(mappedBy = "exchangeRate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions = new ArrayList<>();
}
