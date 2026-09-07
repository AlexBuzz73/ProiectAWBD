package com.example.transactionservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequestDTO {

    @NotNull(message = "Contul sursa trebuie selectat.")
    private Long sourceAccountId;

    @NotBlank(message = "IBAN-ul destinatie este obligatoriu.")
    @Pattern(regexp = "^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$", message = "Format IBAN invalid.")
    private String destinationIban;

    @NotNull(message = "Suma este obligatorie.")
    @DecimalMin(value = "0.01", message = "Suma trebuie sa fie mai mare decat 0.")
    private BigDecimal amount;

    @NotBlank(message = "Valuta este obligatorie.")
    private String currency;

    @NotNull(message = "Categoria este obligatorie.")
    private Integer categoryId;

    @NotBlank(message = "Tipul de procesare este obligatoriu.")
    private String processingType;

    private String description;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date scheduledDate;

    private List<Integer> tagIds;

    private String password;
}
