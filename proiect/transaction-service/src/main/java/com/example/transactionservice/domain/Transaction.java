package com.example.transactionservice.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "initiated_by_user_id", nullable = false)
    private Integer initiatedByUserId;

    @Column(name = "source_account_id")
    private Long sourceAccountId;

    @Column(name = "source_account_iban", length = 34)
    private String sourceAccountIban;

    @Column(name = "source_account_alias", length = 100)
    private String sourceAccountAlias;

    @Column(name = "destination_account_id")
    private Long destinationAccountId;

    @Column(name = "destination_account_iban", length = 34)
    private String destinationAccountIban;

    @Column(name = "destination_account_alias", length = 100)
    private String destinationAccountAlias;

    @Column(name = "destination_iban", length = 34)
    private String destinationIban;

    @Column(name = "transaction_type", length = 20)
    private String transactionType;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "is_urgent", length = 3)
    private String isUrgent;

    @Column(name = "is_scheduled", length = 3)
    private String isScheduled;

    @Column(name = "status", length = 30)
    private String status;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private Date updatedAt;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "exchange_rate_id")
    private ExchangeRate exchangeRate;

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL)
    private ScheduledPayment scheduledPayment;

    @ManyToMany
    @JoinTable(
            name = "transaction_tags",
            joinColumns = @JoinColumn(name = "transaction_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags = new ArrayList<>();
}
