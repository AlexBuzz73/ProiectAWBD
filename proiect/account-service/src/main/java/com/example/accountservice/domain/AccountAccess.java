package com.example.accountservice.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "account_access", uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "user_id"}))
public class AccountAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountAccessId;

    @Column(name = "access_role", nullable = false)
    private String accessRole;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

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
