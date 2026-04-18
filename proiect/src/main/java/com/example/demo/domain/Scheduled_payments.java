package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
public class Scheduled_payments {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int scheduled_payment_id;
    private int transaction_id;
    private Date scheduled_date;
    private String status;
    private Date created_at;
    private Date updated_at;

    public Scheduled_payments() {
    }

    @OneToOne
    private Transaction transaction;
}
