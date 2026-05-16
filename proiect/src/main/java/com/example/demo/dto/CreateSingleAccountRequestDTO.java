package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CreateSingleAccountRequestDTO {

    @NotBlank(message = "Account alias is required!")
    private String alias;
    @NotBlank(message = "Account currency is required!")
    private String currency;
    @NotBlank(message = "External IBAN is required!")
    @Size(min = 24, max = 24, message = "External IBAN must contain 24 characters!")
    private String externalIban;
    @Positive(message = "Initial amount must be positive!")
    private double initialAmount;
}
