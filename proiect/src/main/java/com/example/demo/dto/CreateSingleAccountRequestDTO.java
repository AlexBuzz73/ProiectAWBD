package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CreateSingleAccountRequestDTO {
    private String alias;
    private String currency;
    private String externalIban;
    private double initialAmount;
}
