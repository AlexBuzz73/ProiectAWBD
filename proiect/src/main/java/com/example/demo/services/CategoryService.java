package com.example.demo.services;

import com.example.demo.dto.CategoryRequestDTO;
import com.example.demo.dto.CategoryResponseDTO;

import java.util.List;

public interface CategoryService {
    public List<CategoryResponseDTO> getAvailableCategories(Integer userId);
    public CategoryResponseDTO createCategory(Integer userId, CategoryRequestDTO categoryRequestDTO);
    public void deleteCategory(Integer userId, Integer categoryId);
}
