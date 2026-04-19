package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "scheduled_payments")
public class ScheduledPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int scheduledPaymentId;
    @Temporal(TemporalType.TIMESTAMP)
    private Date scheduledDate;
    private String status;
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    public ScheduledPayment() {
    }

    @OneToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;
}