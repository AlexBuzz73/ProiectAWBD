package com.example.transactionservice.services;

import com.example.transactionservice.dto.CategoryRequestDTO;
import com.example.transactionservice.dto.CategoryResponseDTO;
import com.example.transactionservice.dto.PageResponseDTO;

import java.util.List;

public interface CategoryService {
    List<CategoryResponseDTO> getAvailableCategories(Integer userId);
    CategoryResponseDTO createCategory(Integer userId, CategoryRequestDTO categoryRequestDTO);
    void deleteCategory(Integer userId, Integer categoryId);
    void updateCategory(Integer userId, Integer categoryId, CategoryRequestDTO categoryRequestDTO);
    CategoryResponseDTO getCategory(Integer userId, Integer categoryId);
    PageResponseDTO<CategoryResponseDTO> getAvailableCategoriesPaged(Integer userId, int page, int size, String sortBy, String direction);
}
