package com.example.ems.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.ems.entity.Income;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface IncomeRepository extends JpaRepository<Income, UUID>, JpaSpecificationExecutor<Income> {
	@Query("SELECT SUM(i.amount) FROM Income i WHERE i.user.id = :userId AND i.incomeDate BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalAmountByDateRange(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

	@Query("SELECT YEAR(i.incomeDate), MONTH(i.incomeDate), SUM(i.amount) " +
	           "FROM Income i " +
	           "WHERE i.user.id = :userId " +
	           "AND i.incomeDate BETWEEN :startDate AND :endDate " +
	           "GROUP BY YEAR(i.incomeDate), MONTH(i.incomeDate) " +
	           "ORDER BY YEAR(i.incomeDate), MONTH(i.incomeDate)")
	    List<Object[]> sumAmountGroupByMonthRaw(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

	    @EntityGraph(attributePaths = {"user", "category"})
	    @Query("SELECT i FROM Income i " +
	           "WHERE (:userId IS NULL OR i.user.id = :userId) " +
	           "AND (:categoryId IS NULL OR i.category.id = :categoryId) " +
	           "AND (:startDate IS NULL OR i.incomeDate >= :startDate) " +
	           "AND (:endDate IS NULL OR i.incomeDate <= :endDate) " +
	           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
	    Page<Income> searchIncomes(
	            @Param("userId") UUID userId,
	            @Param("categoryId") UUID categoryId,
	            @Param("startDate") LocalDate startDate,
	            @Param("endDate") LocalDate endDate,
	            @Param("keyword") String keyword,
	            Pageable pageable);
}
