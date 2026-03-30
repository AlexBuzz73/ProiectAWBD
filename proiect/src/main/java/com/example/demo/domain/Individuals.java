package com.example.demo.domain;


import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
public class Individuals {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int individual_id;
    private String first_name;
    private String last_name;
    private String CNP;
    private Date date_of_birth;
    private String phone_number;

    public Individuals() {
    }

    @OneToOne
    private Users users;
}
