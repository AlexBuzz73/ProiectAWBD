package com.example.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private Integer userId;
    private String username;
    private String email;
    private String role;
    private String status;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Date dateOfBirth;
}
