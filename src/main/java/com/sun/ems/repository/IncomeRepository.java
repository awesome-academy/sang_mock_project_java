package com.sun.ems.repository;

import com.sun.ems.entity.Income;
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
}
