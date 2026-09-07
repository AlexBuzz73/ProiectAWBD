package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryRequestDTO {
    private String email;

    @NotBlank(message = "Category name is required!")
    @Size(min = 1, max = 50, message = "Category name must be between 1 and 50 characters!")
    private String name;
}

