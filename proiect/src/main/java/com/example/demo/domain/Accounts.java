package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
public class Accounts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long account_id;
    private String iban;
    private String type;
    private String currency;
    private double balance;
    private String alias;

    public Accounts() {
    }

    @ManyToMany(mappedBy = "accounts")
    private List<Users> users;

    @OneToMany(mappedBy = "card_account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cards> cards;

    @OneToMany(mappedBy = "transaction_account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions;

}
