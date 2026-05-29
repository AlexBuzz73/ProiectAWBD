package com.example.demo.repositories;

import com.example.demo.domain.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    List<Category> findByIsSystemAndStatus(String isSystem, String status);
    List<Category> findByCreatedByUserUserIdAndStatus(Integer userId, String status);
    boolean existsByNameAndIsSystemAndStatus(String name, String isSystem, String status);
    boolean existsByNameAndCreatedByUserUserIdAndStatus(String name, Integer userId, String status);
    @Query("""
    select c
    from   Category c
    where  c.status = 'ACTIVE'
    and    (
              c.isSystem = 'Y'
              or c.createdByUser.userId = :userId
    )
""")
    Page<Category> findAvailableCategories(@Param("userId") Integer userId, Pageable pageable);
    @Query("""
    select    c
    from      Category c
    left join c.transactions t
    where     c.status = 'ACTIVE'
    and       (
                  c.isSystem = 'Y'
                  or c.createdByUser.userId = :userId
        )
    group by  c
    order by  count(t) asc
""")
    Page<Category> findAvailableCategoriesOrderByUsageCountAsc(@Param("userId") Integer userId, Pageable pageable);
    @Query("""
    select    c
    from      Category c
    left join c.transactions t
    where     c.status = 'ACTIVE'
    and       (
                  c.isSystem = 'Y'
                  or c.createdByUser.userId = :userId
        )
    group by  c
    order by  count(t) desc
""")
    Page<Category> findAvailableCategoriesOrderByUsageCountDesc(@Param("userId") Integer userId, Pageable pageable);
}
