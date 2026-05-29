package com.example.demo.services;

import com.example.demo.dto.CategoryRequestDTO;
import com.example.demo.dto.CategoryResponseDTO;
import com.example.demo.dto.PageResponseDTO;

import java.util.List;

public interface CategoryService {
    public List<CategoryResponseDTO> getAvailableCategories(Integer userId);
    public CategoryResponseDTO createCategory(Integer userId, CategoryRequestDTO categoryRequestDTO);
    public void deleteCategory(Integer userId, Integer categoryId);
    public void updateCategory(Integer userId, Integer categoryId, CategoryRequestDTO categoryRequestDTO);
    public CategoryResponseDTO getCategory(Integer userId, Integer categoryId);
    public PageResponseDTO<CategoryResponseDTO> getAvailableCategoriesPaged(Integer userId, int page, int size, String sortBy, String direction);
}
