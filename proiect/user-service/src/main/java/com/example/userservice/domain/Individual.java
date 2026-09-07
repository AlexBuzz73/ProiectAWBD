package com.example.userservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "individuals")
public class Individual {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer individualId;

    private String firstName;
    private String lastName;

    @Column(unique = true, nullable = false)
    private String cnp;

    private String phoneNumber;

    @Temporal(TemporalType.DATE)
    private Date dateOfBirth;

    private String status;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @OneToOne(mappedBy = "individual")
    private User user;
}
