package com.example.transactionservice.mappers;

import com.example.transactionservice.domain.Category;
import com.example.transactionservice.dto.CategoryResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponseDTO toCategoryResponseDTO(Category category) {
        if (category == null) return null;
        return new CategoryResponseDTO(
                category.getCategoryId(),
                category.getName(),
                category.getIsSystem(),
                category.getStatus()
        );
    }
}
