package com.example.accountservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SharedAccountRequest {

    @NotBlank(message = "Aliasul contului este obligatoriu.")
    private String alias;

    @NotBlank(message = "Valuta este obligatorie.")
    @Pattern(regexp = "^(?i)(RON|EUR|USD)$", message = "Valuta trebuie să fie RON, EUR sau USD.")
    private String currency;

    @NotEmpty(message = "Trebuie specificat cel puțin un utilizator.")
    @Size(min = 1, max = 2, message = "Un cont partajat poate avea maxim 2 utilizatori.")
    @Valid
    private List<UserRoleDTO> users;
}
