package com.example.ems.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.ems.entity.Budget;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    List<Budget> findByUserId(@Param("userId") UUID userId);
    
    List<Budget> findByUserIdAndPeriod(@Param("userId") UUID userId, @Param("period") String period);

    boolean existsByUserIdAndCategoryIdAndPeriod(@Param("userId") UUID userId, @Param("categoryId") UUID categoryId, @Param("period") String period);

    boolean existsByUserIdAndCategoryIsNullAndPeriod(@Param("userId") UUID userId, @Param("period") String period);
    
    boolean existsByUserIdAndCategoryIdAndPeriodAndIdNot(@Param("userId") UUID userId, @Param("categoryId") UUID categoryId, @Param("period") String period, @Param("id") UUID id);

    boolean existsByUserIdAndCategoryIsNullAndPeriodAndIdNot(@Param("userId") UUID userId, @Param("period") String period, @Param("id") UUID id);

    List<Budget> findByUserIdAndCategoryIsNullAndPeriodIn(UUID userId, Set<String> periods);
}
