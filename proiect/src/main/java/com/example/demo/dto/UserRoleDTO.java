package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserRoleDTO {
    @NotBlank(message = "Email is required!")
    @Email(message = "Invalid email format!")
    private String email;

    @NotBlank(message = "Role is required!")
    @Pattern(regexp = "^(?i)(OWNER|CO_OWNER|VIEWER)$", message = "Role must be OWNER, CO_OWNER, or VIEWER!")
    private String role;
}