package com.example.demo.services.impl;

import com.example.demo.domain.Category;
import com.example.demo.domain.User;
import com.example.demo.dto.CategoryRequestDTO;
import com.example.demo.dto.CategoryResponseDTO;
import com.example.demo.mappers.CategoryMapper;
import com.example.demo.repositories.CategoryRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.CategoryService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class CategoryServiceImpl implements CategoryService {
    private UserRepository userRepository;
    private CategoryRepository categoryRepository;
    private CategoryMapper categoryMapper;

    @Override
    public List<CategoryResponseDTO> getAvailableCategories(Integer userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        List<Category> systemCategories = categoryRepository.findByIsSystemAndStatus("Y", "ACTIVE");

        List<Category> userCategories = categoryRepository.findByCreatedByUserUserIdAndStatus(userId, "ACTIVE");

        List<CategoryResponseDTO> categoryResponseDTOList = new ArrayList<>();

        systemCategories.forEach(category -> categoryResponseDTOList.add(categoryMapper.toCategoryResponseDTO(category, "N")) );

        userCategories.forEach(category -> categoryResponseDTOList.add(categoryMapper.toCategoryResponseDTO(category, "Y")) );

        return categoryResponseDTOList;

    }

    @Override
    public CategoryResponseDTO createCategory(Integer userId, CategoryRequestDTO categoryRequestDTO) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        boolean systemCategories = categoryRepository.existsByNameAndIsSystemAndStatus(categoryRequestDTO.getName(), "Y", "ACTIVE");

        if (systemCategories){
            throw new RuntimeException("Category already exists with the same name");
        }

        boolean userCategoryExists = categoryRepository.existsByNameAndCreatedByUserUserIdAndStatus(categoryRequestDTO.getName(), userId, "ACTIVE");

        if (userCategoryExists){
            throw new RuntimeException("Category already exists with the same name");
        }

        Category category = new Category();

        category.setName(categoryRequestDTO.getName());
        category.setIsSystem("N");
        category.setCreatedByUser(user);
        category.setStatus("ACTIVE");
        category.setCreatedAt(new Date());
        category.setUpdatedAt( new Date());

        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.toCategoryResponseDTO(savedCategory, "Y");
    }

    @Override
    public void deleteCategory(Integer userId, Integer categoryId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new RuntimeException("Category not found"));

        if ("Y".equals(category.getIsSystem())){
            throw new RuntimeException("System categories cannot be deleted");
        }

        if (category.getCreatedByUser() == null || !category.getCreatedByUser().equals(user)){
            throw new RuntimeException("You can delete your own category");
        }



        category.setStatus("INACTIVE");
        category.setUpdatedAt( new Date());

        categoryRepository.save(category);
    }

    @Override
    public void updateCategory(Integer userId, Integer categoryId, CategoryRequestDTO categoryRequestDTO) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new RuntimeException("Category not found"));

        if (category.getCreatedByUser() == null || !category.getCreatedByUser().equals(user)){
            throw new RuntimeException("You can update your own category");
        }


        category.setName(categoryRequestDTO.getName());
        category.setIsSystem("Y");
        category.setStatus("ACTIVE");
        category.setUpdatedAt(new Date());

        categoryRepository.save(category);
    }

    @Override
    public CategoryResponseDTO getCategory(Integer userId, Integer categoryId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new RuntimeException("Category not found"));

        return  categoryMapper.toCategoryResponseDTO(category, "Y");
    }
}
