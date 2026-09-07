package com.example.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CardResponseDTO {
    private int cardId;
    private String cardNumber;
    private String type;
    private Date expirationDate;
    private String holderName;
    private String status;
}
