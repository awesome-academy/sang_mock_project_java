package com.sun.ems.service.api;

import com.sun.ems.dto.response.ChartData;
import com.sun.ems.dto.response.ReportStats;
import com.sun.ems.entity.User;
import com.sun.ems.exception.InvalidDateException;
import com.sun.ems.repository.ExpenseRepository;
import com.sun.ems.repository.IncomeRepository;
import com.sun.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    /**
     * Retrieves overall statistics (Total Income, Total Expense, Balance) within a specific date range.
     *
     * @param startDate The start date of the reporting period.
     * @param endDate   The end date of the reporting period.
     * @return A ReportStats object containing calculated totals.
     */
    @Transactional(readOnly = true)
    public ReportStats getStats(LocalDate startDate, LocalDate endDate) {
        User user = getCurrentUser();
        
        if (startDate.isAfter(endDate)) {
            throw new InvalidDateException("Start date must be before end date");
        }

        BigDecimal totalExpense = expenseRepository.sumTotalAmountByDateRange(user.getId(), startDate, endDate);
        BigDecimal totalIncome = incomeRepository.sumTotalAmountByDateRange(user.getId(), startDate, endDate);

        if (totalExpense == null) totalExpense = BigDecimal.ZERO;
        if (totalIncome == null) totalIncome = BigDecimal.ZERO;

        BigDecimal balance = totalIncome.subtract(totalExpense);

        return ReportStats.builder()
                .totalExpense(totalExpense)
                .totalIncome(totalIncome)
                .balance(balance)
                .build();
    }

    /**
     * Retrieves data for the expense distribution circle chart (Pie Chart).
     * Groups expenses by category.
     *
     * @param startDate The start date filter.
     * @param endDate   The end date filter.
     * @return List of ChartData (Label: Category Name, Value: Total Amount).
     */
    @Transactional(readOnly = true)
    public List<ChartData> getExpenseCategoryChart(LocalDate startDate, LocalDate endDate) {
        User user = getCurrentUser();
        
        if (startDate.isAfter(endDate)) {
            throw new InvalidDateException("Start date must be before end date");
        }
        
        return expenseRepository.sumAmountGroupByCategory(user.getId(), startDate, endDate);
    }

    private List<ChartData> mapToChartData(List<Object[]> rawData) {
        return rawData.stream().map(obj -> {
            Integer year = (Integer) obj[0];
            Integer month = (Integer) obj[1];
            BigDecimal amount = (BigDecimal) obj[2];

            String label = String.format("%02d-%d", month, year);
            
            return new ChartData(label, amount);
        }).collect(Collectors.toList());
    }

    /**
     * Retrieves the expense history data aggregated by month within a specified date range
     *
     * @param startDate The start date filter.
     * @param endDate   The end date filter.
     * @return List of ChartData (Label: Category Name, Value: Total Amount).
     */
    @Transactional(readOnly = true)
    public List<ChartData> getExpenseHistoryChart(LocalDate startDate, LocalDate endDate) {
        User user = getCurrentUser();
        
        if (startDate.isAfter(endDate)) {
            throw new InvalidDateException("Start date must be before end date");
        }
        
        List<Object[]> rawData = expenseRepository.sumAmountGroupByMonthRaw(user.getId(), startDate, endDate);

        return mapToChartData(rawData);
    }
}
