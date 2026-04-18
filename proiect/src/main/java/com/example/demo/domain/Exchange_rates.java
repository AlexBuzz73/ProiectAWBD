package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Entity
public class Exchange_rates {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int exchange_rate_id;
    private String currency_from;
    private String currency_to;
    private double rate;
    private Date rate_date;
    private String Source;
    private Date created_at;

    public Exchange_rates() {
    }

    @OneToMany(mappedBy = "exchange_rate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions;
}
