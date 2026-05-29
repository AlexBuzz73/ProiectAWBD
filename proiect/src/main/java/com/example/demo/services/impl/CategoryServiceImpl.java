package com.example.demo.services.impl;

import com.example.demo.domain.Category;
import com.example.demo.domain.User;
import com.example.demo.dto.CategoryRequestDTO;
import com.example.demo.dto.CategoryResponseDTO;
import com.example.demo.dto.PageResponseDTO;
import com.example.demo.mappers.CategoryMapper;
import com.example.demo.repositories.CategoryRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryResponseDTO> getAvailableCategories(Integer userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        List<Category> systemCategories = categoryRepository.findByIsSystemAndStatus("Y", "ACTIVE");

        List<Category> userCategories = categoryRepository.findByCreatedByUserUserIdAndStatus(userId, "ACTIVE");

        List<CategoryResponseDTO> categoryResponseDTOList = new ArrayList<>();

        systemCategories.forEach(category -> categoryResponseDTOList.add(categoryMapper.toCategoryResponseDTO(category)));

        userCategories.forEach(category -> categoryResponseDTOList.add(categoryMapper.toCategoryResponseDTO(category)));

        return categoryResponseDTOList;
    }

    @Override
    public CategoryResponseDTO createCategory(Integer userId, CategoryRequestDTO categoryRequestDTO) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        boolean systemCategories = categoryRepository.existsByNameAndIsSystemAndStatus(categoryRequestDTO.getName(), "Y", "ACTIVE");

        if (systemCategories){
            throw new IllegalArgumentException("Category already exists with the same name");
        }

        boolean userCategoryExists = categoryRepository.existsByNameAndCreatedByUserUserIdAndStatus(categoryRequestDTO.getName(), userId, "ACTIVE");

        if (userCategoryExists){
            throw new IllegalArgumentException("Category already exists with the same name");
        }

        Category category = new Category();

        category.setName(categoryRequestDTO.getName());
        category.setIsSystem("N");
        category.setCreatedByUser(user);
        category.setStatus("ACTIVE");
        category.setCreatedAt(new Date());
        category.setUpdatedAt( new Date());

        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.toCategoryResponseDTO(savedCategory);
    }

    @Override
    public void deleteCategory(Integer userId, Integer categoryId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if ("Y".equals(category.getIsSystem())){
            throw new IllegalArgumentException("System categories cannot be deleted");
        }

        if (category.getCreatedByUser() == null || !category.getCreatedByUser().equals(user)){
            throw new IllegalArgumentException("You can delete your own category");
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
        category.setIsSystem("N");
        category.setStatus("ACTIVE");
        category.setUpdatedAt(new Date());

        categoryRepository.save(category);
    }

    @Override
    public CategoryResponseDTO getCategory(Integer userId, Integer categoryId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new RuntimeException("Category not found"));

        return  categoryMapper.toCategoryResponseDTO(category);
    }

    @Override
    public PageResponseDTO<CategoryResponseDTO> getAvailableCategoriesPaged(Integer userId, int page, int size, String sortBy, String direction) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalArgumentException("User is not active!");
        }

        Pageable pageable = createCategoryPageable(page, size, sortBy, direction);

        Page<Category> categoryPage = getCategoryPage(userId, sortBy, direction, pageable);

        List<CategoryResponseDTO> categories = categoryPage.getContent()
                .stream()
                .map(category -> categoryMapper.toCategoryResponseDTO(category))
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
