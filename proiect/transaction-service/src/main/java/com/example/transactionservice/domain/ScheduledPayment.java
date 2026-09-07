package com.example.transactionservice.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "scheduled_payments")
public class ScheduledPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scheduled_payment_id")
    private Integer scheduledPaymentId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "scheduled_date")
    private Date scheduledDate;

    @Column(name = "status", length = 20)
    private String status;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private Date updatedAt;

    @OneToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;
}
