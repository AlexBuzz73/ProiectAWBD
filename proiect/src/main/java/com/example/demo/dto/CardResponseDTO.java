package com.example.demo.dto;

import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CardResponseDTO {
    private String cardNumber;
    private String type;
    private Date expirationDate;
    private String holderName;
    private String status;
}
