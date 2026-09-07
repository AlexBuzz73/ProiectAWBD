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
public class DebitRequestDTO {
    @NotNull(message = "Suma este obligatorie")
    @DecimalMin(value = "0.01", message = "Suma trebuie sa fie cel putin 0.01")
    private BigDecimal amount;

    private String operationId;
}
