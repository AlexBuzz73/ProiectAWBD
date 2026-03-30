package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
public class Cards {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int card_id;
    private double amount;
    private String card_number;
    private String type;
    private Date expiration_date;
    private String CVV;
    private String status;

    public Cards() {
    }

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Accounts card_account;

}
