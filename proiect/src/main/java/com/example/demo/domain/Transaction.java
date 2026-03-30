package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int transaction_id;
    private double amount;
    private String type;
    private String status;
    private String description;
    private String iban_from;
    private String iban_to;

    public Transaction() {
    }

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Accounts transaction_account;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users user;

    @OneToMany(mappedBy = "tag_transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tag> tags;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Categories category;

}
