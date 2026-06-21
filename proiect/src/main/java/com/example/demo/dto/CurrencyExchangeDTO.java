package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CurrencyExchangeDTO {
    @NotNull(message = "Contul sursă este obligatoriu.")
    private Integer sourceAccountId;

    @NotNull(message = "Contul destinație este obligatoriu.")
    private Integer destinationAccountId;

    @Positive(message = "Suma trebuie să fie pozitivă.")
    private double amount;

    @NotNull(message = "Categoria este obligatorie.")
    private Integer categoryId;

    private String description;

    @NotNull(message = "Parola este obligatorie pentru autorizare.")
    private String password;
}