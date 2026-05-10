package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OwnAccountTransferDTO {
    @NotNull(message = "Contul sursă este obligatoriu.")
    private Integer sourceAccountId;

    @NotNull(message = "Contul destinație este obligatoriu.")
    private Integer destinationAccountId;

    @NotNull(message = "Suma este obligatorie.")
    @Positive(message = "Suma trebuie să fie pozitivă.")
    private Double amount;

    @NotNull(message = "Categoria este obligatorie.")
    private Integer categoryId;

    private String description;

    @NotNull(message = "Parola este obligatorie pentru autorizare.")
    private String password;
}