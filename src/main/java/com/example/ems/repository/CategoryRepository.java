package com.example.ems.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.ems.entity.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
	@Query("SELECT c FROM Category c LEFT JOIN FETCH c.user WHERE (c.user.id = :userId OR c.user IS NULL) AND c.isDeleted = false")    
	List<Category> findAllByUserIdOrGlobal(@Param("userId") UUID userId);
    
    Optional<Category> findByIdAndIsDeletedFalse(@Param("id") UUID id);
}
