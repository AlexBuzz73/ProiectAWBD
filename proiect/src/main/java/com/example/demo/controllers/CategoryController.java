package com.example.demo.controllers;

import com.example.demo.services.CurrentUserService;
import com.example.demo.services.ResourceAuthorizationService;

import com.example.demo.dto.CategoryRequestDTO;
import com.example.demo.dto.CategoryResponseDTO;
import com.example.demo.dto.PageResponseDTO;
import com.example.demo.services.CategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/users/{userId}/categories", "/api/users/me/categories"})
public class CategoryController {
    private final CurrentUserService currentUserService;
    private final ResourceAuthorizationService authorization;

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService, CurrentUserService currentUserService, ResourceAuthorizationService authorization) {
        this.categoryService = categoryService;
        this.currentUserService = currentUserService;
        this.authorization = authorization;
    }

    @PostMapping
    public CategoryResponseDTO createCategory(
            @PathVariable(name = "userId", required = false) Integer requestedUserId,
            @Valid @RequestBody  CategoryRequestDTO categoryRequestDTO
    ) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        return categoryService.createCategory(userId, categoryRequestDTO);
    }

    @GetMapping
    public List<CategoryResponseDTO> getAvailableCategories(
            @PathVariable(name = "userId", required = false) Integer requestedUserId
    ) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        return categoryService.getAvailableCategories(userId);
    }

    @GetMapping("/{categoryId}")
    public CategoryResponseDTO getCategory(
            @PathVariable(name = "userId", required = false) Integer requestedUserId,
            @PathVariable Integer categoryId
    ) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        authorization.requireCategory(userId, categoryId, false);
        return categoryService.getCategory(userId, categoryId);
    }

    @DeleteMapping("/{categoryId}")
    public void deleteCategory(
            @PathVariable(name = "userId", required = false) Integer requestedUserId,
            @PathVariable Integer categoryId
    ) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        authorization.requireCategory(userId, categoryId, true);
        categoryService.deleteCategory(userId, categoryId);
    }

    @PutMapping("/{categoryId}")
    public void updateCategory(
            @PathVariable(name = "userId", required = false) Integer requestedUserId,
            @PathVariable Integer categoryId,
            @Valid @RequestBody  CategoryRequestDTO categoryRequestDTO
    ){
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        authorization.requireCategory(userId, categoryId, true);
        categoryService.updateCategory(userId, categoryId, categoryRequestDTO);
    }

    @GetMapping("/paged")
    public PageResponseDTO<CategoryResponseDTO> getAvailableCategoriesPaged(
            @PathVariable(name = "userId", required = false) Integer requestedUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        return categoryService.getAvailableCategoriesPaged(userId, page, size, sortBy, direction);
    }
}
