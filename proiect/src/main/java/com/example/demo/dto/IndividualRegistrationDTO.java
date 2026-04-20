package com.example.demo.dto;

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

    private String firstName;
    private String lastName;
    private String cnp;
    private String phoneNumber;
    private Date dateOfBirth;
}
