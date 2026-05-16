package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class IndividualRegistrationDTO {

    @NotBlank(message = "First name is required!")
    private String firstName;
    @NotBlank(message = "Last name is required!")
    private String lastName;
    @NotBlank(message = "CNP is required!")
    private String cnp;
    @NotBlank(message = "Phone number is required!")
    private String phoneNumber;
    @NotNull(message = "Birth date is required!")
    @Past(message = "Birth date must be in the past!")
    private Date dateOfBirth;
}
