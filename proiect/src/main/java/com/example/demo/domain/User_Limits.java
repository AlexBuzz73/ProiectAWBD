package com.example.demo.domain;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Entity
public class User_Limits {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int user_limit_id;
    private int user_id;
    private int max_amount_per_transaction_ron;
    private int max_daily_amount_ron;
    private int max_daily_transactions_count;
    private String status;
    private Date created_at;
    private Date updated_at;

    public User_Limits() {
    }

    @OneToOne
    private Users users;

}
