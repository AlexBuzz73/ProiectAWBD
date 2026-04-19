package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "account_access", uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "user_id"}))
public class AccountAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountAccessId;
    private String accessRole;
    private String status;
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    public AccountAccess() {
    }

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}