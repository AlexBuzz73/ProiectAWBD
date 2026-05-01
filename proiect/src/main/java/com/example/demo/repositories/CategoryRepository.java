package com.example.demo.repositories;

import com.example.demo.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    List<Category> findByIsSystemAndStatus(String isSystem, String status);
    List<Category> findByCreatedByUserUserIdAndStatus(Integer userId, String status);
    boolean existsByNameAndIsSystemAndStatus(String name, String isSystem, String status);
    boolean existsByNameAndCreatedByUserUserIdAndStatus(String name, Integer userId, String status);
}
