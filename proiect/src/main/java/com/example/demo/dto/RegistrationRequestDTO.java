package com.example.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationRequestDTO {

    @Valid
    @NotNull(message = "Individual data is required!")
    private IndividualRegistrationDTO individual;
    @Valid
    @NotNull(message = "User data is required!")
    private UserRegistrationDTO user;
}
