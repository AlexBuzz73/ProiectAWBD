package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "cards")
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int cardId;
    private String cardNumber;
    private String type;
    @Temporal(TemporalType.DATE)
    private Date expirationDate;
    private String holderName;
    private String status;
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    public Card() {
    }

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;
}