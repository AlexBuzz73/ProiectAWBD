package com.example.accountservice.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bank_limits")
public class BankLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer bankLimitId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal maxAmountPerTransactionRon;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal maxDailyAmountRon;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal maxDailyTransactionsCount;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date updatedAt;

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
}
