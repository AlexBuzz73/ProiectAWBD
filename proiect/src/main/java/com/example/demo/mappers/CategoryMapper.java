package com.example.demo.mappers;

import com.example.demo.domain.Category;
import com.example.demo.dto.CategoryResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {
    public CategoryResponseDTO toCategoryResponseDTO(Category category) {
        CategoryResponseDTO categoryResponseDTO = new CategoryResponseDTO();
        categoryResponseDTO.setCategoryId(category.getCategoryId());
        categoryResponseDTO.setName(category.getName());
        categoryResponseDTO.setIsSystem(category.getIsSystem());
        categoryResponseDTO.setStatus(category.getStatus());

        return categoryResponseDTO;
    }
}
