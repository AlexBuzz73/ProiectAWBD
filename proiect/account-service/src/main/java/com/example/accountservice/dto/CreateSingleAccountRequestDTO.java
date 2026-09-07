package com.example.accountservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateSingleAccountRequestDTO {

    @NotBlank(message = "Account alias is required!")
    private String alias;

    @NotBlank(message = "Account currency is required!")
    @Pattern(regexp = "^(?i)(RON|EUR|USD)$", message = "Unsupported currency!")
    private String currency;

    private String externalIban;

    private double initialAmount = 0.0;
}
