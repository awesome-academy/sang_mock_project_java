package com.example.ems.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.ems.constant.CategoryType;
import com.example.ems.entity.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
	@Query("SELECT c FROM Category c LEFT JOIN FETCH c.user WHERE (c.user.id = :userId OR c.user IS NULL) AND c.isDeleted = false")    
	List<Category> findAllByUserIdOrGlobal(@Param("userId") UUID userId);
    
    Optional<Category> findByIdAndIsDeletedFalse(@Param("id") UUID id);
    
    @Query("SELECT c FROM Category c WHERE " +
            "c.user IS NULL " +
            "AND c.isDeleted = false " +
            "AND (:keyword IS NULL OR :keyword = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:type IS NULL OR c.type = :type)")
     Page<Category> searchGlobalCategories(@Param("keyword") String keyword, @Param("type") CategoryType type, Pageable pageable);

     @Query("SELECT COUNT(c) > 0 FROM Category c WHERE " +
            "c.user IS NULL " +
            "AND c.isDeleted = false " +
            "AND LOWER(c.name) = LOWER(:name) " +
            "AND (:excludeId IS NULL OR c.id != :excludeId)")
     boolean existsByNameGlobal(@Param("name") String name, @Param("excludeId") UUID excludeId);
}
