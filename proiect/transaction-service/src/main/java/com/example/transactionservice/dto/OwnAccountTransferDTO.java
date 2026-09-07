package com.example.transactionservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OwnAccountTransferDTO {
    @NotNull(message = "Contul sursă este obligatoriu.")
    private Long sourceAccountId;

    @NotNull(message = "Contul destinație este obligatoriu.")
    private Long destinationAccountId;

    @NotNull(message = "Suma este obligatorie.")
    @DecimalMin(value = "0.01", message = "Suma trebuie să fie pozitivă.")
    private BigDecimal amount;

    @NotNull(message = "Categoria este obligatorie.")
    private Integer categoryId;

    private String description;

    private String password;
}
