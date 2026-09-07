package com.example.transactionservice.services.impl;

import com.example.transactionservice.domain.Category;
import com.example.transactionservice.dto.CategoryRequestDTO;
import com.example.transactionservice.dto.CategoryResponseDTO;
import com.example.transactionservice.dto.PageResponseDTO;
import com.example.transactionservice.exceptions.ResourceNotFoundException;
import com.example.transactionservice.mappers.CategoryMapper;
import com.example.transactionservice.repositories.CategoryRepository;
import com.example.transactionservice.services.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getAvailableCategories(Integer userId) {
        List<Category> systemCategories = categoryRepository.findByIsSystemAndStatus("Y", "ACTIVE");
        List<Category> userCategories = categoryRepository.findByCreatedByUserIdAndStatus(userId, "ACTIVE");

        List<CategoryResponseDTO> result = new ArrayList<>();
        systemCategories.forEach(c -> result.add(categoryMapper.toCategoryResponseDTO(c)));
        userCategories.forEach(c -> result.add(categoryMapper.toCategoryResponseDTO(c)));
        return result;
    }

    @Override
    @Transactional
    public CategoryResponseDTO createCategory(Integer userId, CategoryRequestDTO dto) {
        if (categoryRepository.existsByNameAndIsSystemAndStatus(dto.getName(), "Y", "ACTIVE")) {
            throw new IllegalArgumentException("Category already exists with the same name");
        }

        if (categoryRepository.existsByNameAndCreatedByUserIdAndStatus(dto.getName(), userId, "ACTIVE")) {
            throw new IllegalArgumentException("Category already exists with the same name");
        }

        Category category = new Category();
        category.setName(dto.getName());
        category.setIsSystem("N");
        category.setCreatedByUserId(userId);
        category.setStatus("ACTIVE");
        category.setCreatedAt(new Date());
        category.setUpdatedAt(new Date());

        Category saved = categoryRepository.save(category);
        return categoryMapper.toCategoryResponseDTO(saved);
    }

    @Override
    @Transactional
    public void deleteCategory(Integer userId, Integer categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if ("Y".equals(category.getIsSystem())) {
            throw new IllegalArgumentException("System categories cannot be deleted");
        }

        if (category.getCreatedByUserId() == null || !category.getCreatedByUserId().equals(userId)) {
            throw new IllegalArgumentException("You can delete your own category");
        }

        category.setStatus("INACTIVE");
        category.setUpdatedAt(new Date());
        categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void updateCategory(Integer userId, Integer categoryId, CategoryRequestDTO dto) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (category.getCreatedByUserId() == null || !category.getCreatedByUserId().equals(userId)) {
            throw new IllegalArgumentException("You can update your own category");
        }

        category.setName(dto.getName());
        category.setIsSystem("N");
        category.setStatus("ACTIVE");
        category.setUpdatedAt(new Date());
        categoryRepository.save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDTO getCategory(Integer userId, Integer categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        return categoryMapper.toCategoryResponseDTO(category);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<CategoryResponseDTO> getAvailableCategoriesPaged(Integer userId, int page, int size, String sortBy, String direction) {
        Pageable pageable = createCategoryPageable(page, size, sortBy, direction);
        Page<Category> categoryPage = getCategoryPage(userId, sortBy, direction, pageable);

        List<CategoryResponseDTO> categories = categoryPage.getContent()
                .stream()
                .map(categoryMapper::toCategoryResponseDTO)
                .toList();

        return new PageResponseDTO<>(
                categories,
                categoryPage.getNumber(),
                categoryPage.getSize(),
                categoryPage.getTotalElements(),
                categoryPage.getTotalPages(),
                categoryPage.isFirst(),
                categoryPage.isLast()
        );
    }

    private Pageable createCategoryPageable(int page, int size, String sortBy, String direction) {
        if ("name".equals(sortBy)) {
            Sort.Direction sortDirection;
            if ("asc".equalsIgnoreCase(direction)) {
                sortDirection = Sort.Direction.ASC;
            } else if ("desc".equalsIgnoreCase(direction)) {
                sortDirection = Sort.Direction.DESC;
            } else {
                throw new IllegalArgumentException("Invalid sort direction!");
            }
            return PageRequest.of(page, size, Sort.by(sortDirection, "name"));
        }

        if ("usageCount".equals(sortBy)) {
            return PageRequest.of(page, size);
        }

        throw new IllegalArgumentException("Invalid category sort field!");
    }

    private Page<Category> getCategoryPage(Integer userId, String sortBy, String direction, Pageable pageable) {
        if ("name".equals(sortBy)) {
            return categoryRepository.findAvailableCategories(userId, pageable);
        }

        if ("usageCount".equals(sortBy) && "asc".equalsIgnoreCase(direction)) {
            return categoryRepository.findAvailableCategoriesOrderByUsageCountAsc(userId, pageable);
        }

        if ("usageCount".equals(sortBy) && "desc".equalsIgnoreCase(direction)) {
            return categoryRepository.findAvailableCategoriesOrderByUsageCountDesc(userId, pageable);
        }

        throw new IllegalArgumentException("Invalid category sort option!");
    }
}
