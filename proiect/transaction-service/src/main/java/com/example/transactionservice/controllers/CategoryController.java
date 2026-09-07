package com.example.transactionservice.controllers;

import com.example.transactionservice.dto.CategoryRequestDTO;
import com.example.transactionservice.dto.CategoryResponseDTO;
import com.example.transactionservice.dto.PageResponseDTO;
import com.example.transactionservice.services.CategoryService;
import com.example.transactionservice.services.CurrentUserService;
import com.example.transactionservice.services.ResourceAuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/users/{userId}/categories", "/api/users/me/categories", "/api/categories"})
@RequiredArgsConstructor
public class CategoryController {

    private final CurrentUserService currentUserService;
    private final ResourceAuthorizationService authorization;
    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @PathVariable(name = "userId", required = false) Integer requestedUserId,
            @Valid @RequestBody CategoryRequestDTO categoryRequestDTO
    ) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        CategoryResponseDTO response = categoryService.createCategory(userId, categoryRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> getAvailableCategories(
            @PathVariable(name = "userId", required = false) Integer requestedUserId
    ) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        return ResponseEntity.ok(categoryService.getAvailableCategories(userId));
    }

    @GetMapping("/{categoryId:\\d+}")
    public ResponseEntity<CategoryResponseDTO> getCategory(
            @PathVariable(name = "userId", required = false) Integer requestedUserId,
            @PathVariable Integer categoryId
    ) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        authorization.requireCategory(userId, categoryId, false);
        return ResponseEntity.ok(categoryService.getCategory(userId, categoryId));
    }

    @DeleteMapping("/{categoryId:\\d+}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable(name = "userId", required = false) Integer requestedUserId,
            @PathVariable Integer categoryId
    ) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        authorization.requireCategory(userId, categoryId, true);
        categoryService.deleteCategory(userId, categoryId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{categoryId:\\d+}")
    public ResponseEntity<Void> updateCategory(
            @PathVariable(name = "userId", required = false) Integer requestedUserId,
            @PathVariable Integer categoryId,
            @Valid @RequestBody CategoryRequestDTO categoryRequestDTO
    ) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        authorization.requireCategory(userId, categoryId, true);
        categoryService.updateCategory(userId, categoryId, categoryRequestDTO);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/paged")
    public ResponseEntity<PageResponseDTO<CategoryResponseDTO>> getAvailableCategoriesPaged(
            @PathVariable(name = "userId", required = false) Integer requestedUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Integer userId = currentUserService.requireCurrentUserId(requestedUserId);
        return ResponseEntity.ok(categoryService.getAvailableCategoriesPaged(userId, page, size, sortBy, direction));
    }
}
