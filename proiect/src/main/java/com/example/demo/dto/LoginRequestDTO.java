package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginRequestDTO {

    @NotBlank(message = "Email is required!")
    @Email(message = "Invalid email format")
    private String email;
    @NotBlank(message = "Password is required!")
    private String password;
    private Boolean rememberMe;

    public LoginRequestDTO(String email, String password) {
        this.email = email;
        this.password = password;
        this.rememberMe = false;
    }

    public LoginRequestDTO(String email, String password, Boolean rememberMe) {
        this.email = email;
        this.password = password;
        this.rememberMe = rememberMe;
    }
}
