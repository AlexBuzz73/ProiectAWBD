package com.example.demo.controllers;

import com.example.demo.dto.CardResponseDTO;
import com.example.demo.dto.CategoryRequestDTO;
import com.example.demo.dto.CategoryResponseDTO;
import com.example.demo.dto.PageResponseDTO;
import com.example.demo.services.CategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public CategoryResponseDTO createCategory(
            @PathVariable Integer userId,
            @Valid @RequestBody  CategoryRequestDTO categoryRequestDTO
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

    @DeleteMapping("/{categoryId}")
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
            @Valid @RequestBody  CategoryRequestDTO categoryRequestDTO
    ){
        categoryService.updateCategory(userId, categoryId, categoryRequestDTO);
    }

    @GetMapping("/paged")
    public PageResponseDTO<CategoryResponseDTO> getAvailableCategoriesPaged(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        return categoryService.getAvailableCategoriesPaged(userId, page, size, sortBy, direction);
    }
}
