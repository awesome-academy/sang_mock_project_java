package com.sun.ems.repository;

import com.sun.ems.dto.response.ChartData;
import com.sun.ems.entity.Expense;
import com.sun.ems.repository.projection.MonthlyStat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID>, JpaSpecificationExecutor<Expense>{
    List<Expense> findByUserIdOrderByExpenseDateDesc(@Param("userId")  UUID userId);

    @Query("SELECT SUM(e.amount) FROM Expense e " +
           "WHERE e.user.id = :userId " +
           "AND e.category.id = :categoryId " +
           "AND e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByCategoryAndDateRange(@Param("userId") UUID userId, @Param("categoryId") UUID categoryId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT SUM(e.amount) FROM Expense e " +
           "WHERE e.user.id = :userId " +
           "AND e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalAmountByDateRange(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT YEAR(e.expenseDate) as year, MONTH(e.expenseDate) as month, SUM(e.amount) as totalAmount " +
            "FROM Expense e " +
            "WHERE e.user.id = :userId " +
            "AND e.expenseDate BETWEEN :startDate AND :endDate " +
            "GROUP BY YEAR(e.expenseDate), MONTH(e.expenseDate)")
     List<MonthlyStat> findMonthlyStats(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT new com.sun.ems.dto.response.ChartData(e.category.name, SUM(e.amount)) " +
            "FROM Expense e " +
            "WHERE e.user.id = :userId " +
            "AND e.expenseDate BETWEEN :startDate AND :endDate " +
            "GROUP BY e.category.name")
     List<ChartData> sumAmountGroupByCategory(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT YEAR(e.expenseDate), MONTH(e.expenseDate), SUM(e.amount) " +
            "FROM Expense e " +
            "WHERE e.user.id = :userId " +
            "AND e.expenseDate BETWEEN :startDate AND :endDate " +
            "GROUP BY YEAR(e.expenseDate), MONTH(e.expenseDate) " +
            "ORDER BY YEAR(e.expenseDate), MONTH(e.expenseDate)")
    List<Object[]> sumAmountGroupByMonthRaw(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
