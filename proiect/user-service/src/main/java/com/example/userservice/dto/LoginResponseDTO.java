package com.example.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
    private String tokenType; // "Bearer"
    private long expiresIn;   // seconds
    private Integer userId;
    private String username;
    private String email;
    private String role;
}
