package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserRegistrationDTO {

    @NotBlank(message = "Username is required!")
    private String username;
    @NotBlank(message = "Email is required!")
    @Email(message = "Invalid email format!")
    private String email;
    @NotBlank(message = "Password is required!")
    @Size(min = 8, message = "The password must be at least 8 characters long!")
    private String password;
}
