package com.example.demo.services;

import com.example.demo.domain.Category;
import com.example.demo.domain.User;
import com.example.demo.dto.CategoryRequestDTO;
import com.example.demo.dto.CategoryResponseDTO;
import com.example.demo.mappers.CategoryMapper;
import com.example.demo.repositories.CategoryRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private User user;
    private User otherUser;
    private Category systemCategory;
    private Category userCategory;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1);
        user.setUsername("teo");
        user.setStatus("ACTIVE");

        otherUser = new User();
        otherUser.setUserId(2);
        otherUser.setUsername("other");
        otherUser.setStatus("ACTIVE");

        systemCategory = new Category();
        systemCategory.setCategoryId(1);
        systemCategory.setName("Food");
        systemCategory.setIsSystem("Y");
        systemCategory.setStatus("ACTIVE");

        userCategory = new Category();
        userCategory.setCategoryId(2);
        userCategory.setName("Personal");
        userCategory.setIsSystem("N");
        userCategory.setStatus("ACTIVE");
        userCategory.setCreatedByUser(user);
    }

    @Test
    void getAvailableCategories_shouldReturnSystemAndUserCategories() {
        CategoryResponseDTO systemResponse = new CategoryResponseDTO(
                1,
                "Food",
                "N",
                "ACTIVE"
        );

        CategoryResponseDTO userResponse = new CategoryResponseDTO(
                2,
                "Personal",
                "Y",
                "ACTIVE"
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(categoryRepository.findByIsSystemAndStatus("Y", "ACTIVE"))
                .thenReturn(List.of(systemCategory));
        when(categoryRepository.findByCreatedByUserUserIdAndStatus(1, "ACTIVE"))
                .thenReturn(List.of(userCategory));
        when(categoryMapper.toCategoryResponseDTO(systemCategory, "N"))
                .thenReturn(systemResponse);
        when(categoryMapper.toCategoryResponseDTO(userCategory, "Y"))
                .thenReturn(userResponse);

        List<CategoryResponseDTO> result = categoryService.getAvailableCategories(1);

        assertEquals(2, result.size());
        assertEquals("Food", result.get(0).getName());
        assertEquals("Personal", result.get(1).getName());

        verify(categoryRepository).findByIsSystemAndStatus("Y", "ACTIVE");
        verify(categoryRepository).findByCreatedByUserUserIdAndStatus(1, "ACTIVE");
    }

    @Test
    void getAvailableCategories_shouldThrowException_whenUserNotFound() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> categoryService.getAvailableCategories(1)
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void createCategory_shouldCreateCategory_whenNameIsValidAndUnique() {
        CategoryRequestDTO requestDTO = new CategoryRequestDTO();
        requestDTO.setName("Travel");

        Category savedCategory = new Category();
        savedCategory.setCategoryId(3);
        savedCategory.setName("Travel");
        savedCategory.setIsSystem("N");
        savedCategory.setStatus("ACTIVE");
        savedCategory.setCreatedByUser(user);

        CategoryResponseDTO responseDTO = new CategoryResponseDTO(
                3,
                "Travel",
                "Y",
                "ACTIVE"
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(categoryRepository.existsByNameAndIsSystemAndStatus("Travel", "Y", "ACTIVE"))
                .thenReturn(false);
        when(categoryRepository.existsByNameAndCreatedByUserUserIdAndStatus("Travel", 1, "ACTIVE"))
                .thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);
        when(categoryMapper.toCategoryResponseDTO(savedCategory, "Y")).thenReturn(responseDTO);

        CategoryResponseDTO result = categoryService.createCategory(1, requestDTO);

        assertNotNull(result);
        assertEquals("Travel", result.getName());
        assertEquals("ACTIVE", result.getStatus());

        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_shouldThrowException_whenSystemCategoryWithSameNameExists() {
        CategoryRequestDTO requestDTO = new CategoryRequestDTO();
        requestDTO.setName("Food");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(categoryRepository.existsByNameAndIsSystemAndStatus("Food", "Y", "ACTIVE"))
                .thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> categoryService.createCategory(1, requestDTO)
        );

        assertEquals("Category already exists with the same name", exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void createCategory_shouldThrowException_whenUserCategoryWithSameNameExists() {
        CategoryRequestDTO requestDTO = new CategoryRequestDTO();
        requestDTO.setName("Personal");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(categoryRepository.existsByNameAndIsSystemAndStatus("Personal", "Y", "ACTIVE"))
                .thenReturn(false);
        when(categoryRepository.existsByNameAndCreatedByUserUserIdAndStatus("Personal", 1, "ACTIVE"))
                .thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> categoryService.createCategory(1, requestDTO)
        );

        assertEquals("Category already exists with the same name", exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void deleteCategory_shouldSetStatusInactive_whenCategoryBelongsToUser() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(2)).thenReturn(Optional.of(userCategory));
        when(categoryRepository.save(userCategory)).thenReturn(userCategory);

        categoryService.deleteCategory(1, 2);

        assertEquals("INACTIVE", userCategory.getStatus());
        verify(categoryRepository).save(userCategory);
    }

    @Test
    void deleteCategory_shouldThrowException_whenCategoryIsSystemCategory() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(systemCategory));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> categoryService.deleteCategory(1, 1)
        );

        assertEquals("System categories cannot be deleted", exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void deleteCategory_shouldThrowException_whenCategoryBelongsToAnotherUser() {
        Category otherUserCategory = new Category();
        otherUserCategory.setCategoryId(4);
        otherUserCategory.setName("Other");
        otherUserCategory.setIsSystem("N");
        otherUserCategory.setStatus("ACTIVE");
        otherUserCategory.setCreatedByUser(otherUser);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(4)).thenReturn(Optional.of(otherUserCategory));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> categoryService.deleteCategory(1, 4)
        );

        assertEquals("You can delete your own category", exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategory_shouldUpdateCategory_whenCategoryBelongsToUser() {
        CategoryRequestDTO requestDTO = new CategoryRequestDTO();
        requestDTO.setName("Updated");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(2)).thenReturn(Optional.of(userCategory));
        when(categoryRepository.save(userCategory)).thenReturn(userCategory);

        categoryService.updateCategory(1, 2, requestDTO);

        assertEquals("Updated", userCategory.getName());
        assertEquals("ACTIVE", userCategory.getStatus());

        verify(categoryRepository).save(userCategory);
    }

    @Test
    void getCategory_shouldReturnCategoryResponse() {
        CategoryResponseDTO responseDTO = new CategoryResponseDTO(
                2,
                "Personal",
                "Y",
                "ACTIVE"
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(2)).thenReturn(Optional.of(userCategory));
        when(categoryMapper.toCategoryResponseDTO(userCategory, "Y")).thenReturn(responseDTO);

        CategoryResponseDTO result = categoryService.getCategory(1, 2);

        assertNotNull(result);
        assertEquals("Personal", result.getName());
    }
}