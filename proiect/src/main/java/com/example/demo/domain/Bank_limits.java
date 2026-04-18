package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
public class Bank_limits {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int bank_limit_id;
    private int max_amount_per_transaction_ron;
    private int max_daily_amount_ron;
    private int max_daily_transactions_count;
    private String status;
    private Date created_at;
    private Date updated_at;

    public Bank_limits() {
    }

}
