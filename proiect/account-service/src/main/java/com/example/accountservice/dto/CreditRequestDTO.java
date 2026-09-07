package com.example.accountservice.dto;

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
public class CreditRequestDTO {

    @NotNull(message = "Suma pentru creditare este obligatorie.")
    @DecimalMin(value = "0.01", message = "Suma trebuie sa fie mai mare decat 0.")
    private BigDecimal amount;

    private String operationId;
}
