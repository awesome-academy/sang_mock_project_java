package com.example.ems.repository;

import com.example.ems.entity.BudgetTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BudgetTemplateRepository extends JpaRepository<BudgetTemplate, UUID> {

    @Query("SELECT b FROM BudgetTemplate b WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<BudgetTemplate> searchTemplates(String keyword, Pageable pageable);
    
    boolean existsByNameIgnoreCase(String name);
}
