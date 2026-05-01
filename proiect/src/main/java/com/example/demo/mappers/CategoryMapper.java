package com.example.demo.mappers;

import com.example.demo.domain.Category;
import com.example.demo.dto.CategoryResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {
    public CategoryResponseDTO toCategoryResponseDTO(Category category, String isSystem) {
        CategoryResponseDTO categoryResponseDTO = new CategoryResponseDTO();
        categoryResponseDTO.setCategoryId(category.getCategoryId());
        categoryResponseDTO.setName(category.getName());
        categoryResponseDTO.setIsSystem(String.valueOf(isSystem));
        categoryResponseDTO.setStatus(category.getStatus());

        return categoryResponseDTO;
    }
}
