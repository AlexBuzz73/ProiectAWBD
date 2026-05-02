package com.example.demo.controllers;

import com.example.demo.dto.CardResponseDTO;
import com.example.demo.dto.CategoryRequestDTO;
import com.example.demo.dto.CategoryResponseDTO;
import com.example.demo.services.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/categories")
public class CategoryController {

    private CategoryService categoryService;

    @PostMapping
    public CategoryResponseDTO createCategory(
            @PathVariable Integer userId,
            @PathVariable CategoryRequestDTO categoryRequestDTO
    ) {
        return categoryService.createCategory(userId, categoryRequestDTO);
    }

    @GetMapping
    public List<CategoryResponseDTO> getAvailableCategories(
            @PathVariable Integer userId
    ) {
        return categoryService.getAvailableCategories(userId);
    }

    @GetMapping("/{categoryId}")
    public CategoryResponseDTO getCategory(
            @PathVariable Integer userId,
            @PathVariable Integer categoryId
    ) {
        return categoryService.getCategory(userId, categoryId);
    }

    @PatchMapping("/{categoryId}/delete")
    public void deleteCategory(
            @PathVariable Integer userId,
            @PathVariable Integer categoryId
    ) {
        categoryService.deleteCategory(userId, categoryId);
    }

    @PutMapping("/{categoryId}")
    public void updateCategory(
            @PathVariable Integer userId,
            @PathVariable Integer categoryId,
            @PathVariable CategoryRequestDTO categoryRequestDTO
    ){
        categoryService.updateCategory(userId, categoryId, categoryRequestDTO);
    }
}
