package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int tag_id;
    private String tag_name;

    public Tag() {
    }

    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private Transaction tag_transaction;
}
