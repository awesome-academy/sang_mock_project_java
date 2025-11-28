package com.sun.ems.repository;

import com.sun.ems.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    List<Budget> findByUserId(@Param("userId") UUID userId);
    
    List<Budget> findByUserIdAndPeriod(@Param("userId") UUID userId, @Param("period") String period);

    boolean existsByUserIdAndCategoryIdAndPeriod(@Param("userId") UUID userId, @Param("categoryId") UUID categoryId, @Param("period") String period);

    boolean existsByUserIdAndCategoryIsNullAndPeriod(@Param("userId") UUID userId, @Param("period") String period);
    
    boolean existsByUserIdAndCategoryIdAndPeriodAndIdNot(@Param("userId") UUID userId, @Param("categoryId") UUID categoryId, @Param("period") String period, @Param("id") UUID id);

    boolean existsByUserIdAndCategoryIsNullAndPeriodAndIdNot(@Param("userId") UUID userId, @Param("period") String period, @Param("id") UUID id);
}
