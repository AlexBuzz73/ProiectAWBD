package com.example.demo.domain;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@Table(name = "user_limits")
public class UserLimit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userLimitId;
    private BigDecimal maxAmountPerTransactionRon;
    private BigDecimal maxDailyAmountRon;
    private BigDecimal maxDailyTransactionsCount;
    private String status;
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    public UserLimit() {
    }

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;
}