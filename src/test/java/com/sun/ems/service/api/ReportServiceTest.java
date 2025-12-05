package com.sun.ems.service.api;

import com.sun.ems.dto.response.ChartData;
import com.sun.ems.dto.response.ReportStats;
import com.sun.ems.entity.User;
import com.sun.ems.repository.ExpenseRepository;
import com.sun.ems.repository.IncomeRepository;
import com.sun.ems.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private IncomeRepository incomeRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private ReportService reportService;

    private User mockUser;
    private final String TEST_EMAIL = "user@test.com";
    private final LocalDate START_DATE = LocalDate.of(2025, 1, 1);
    private final LocalDate END_DATE = LocalDate.of(2025, 12, 31);

    @BeforeEach
    void setUp() {
        Authentication auth = mock(Authentication.class);
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        when(auth.getName()).thenReturn(TEST_EMAIL);
        SecurityContextHolder.setContext(context);

        mockUser = User.builder().email(TEST_EMAIL).build();
        mockUser.setId(UUID.randomUUID());
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Get Stats: Should calculate totals correctly")
    void getStats_Success() {
        // GIVEN
        BigDecimal income = new BigDecimal("1000");
        BigDecimal expense = new BigDecimal("400");
        
        when(incomeRepository.sumTotalAmountByDateRange(any(), any(), any())).thenReturn(income);
        when(expenseRepository.sumTotalAmountByDateRange(any(), any(), any())).thenReturn(expense);

        // WHEN
        ReportStats stats = reportService.getStats(START_DATE, END_DATE);

        // THEN
        assertEquals(income, stats.getTotalIncome());
        assertEquals(expense, stats.getTotalExpense());
        assertEquals(new BigDecimal("600"), stats.getBalance()); // 1000 - 400 = 600
    }

    @Test
    @DisplayName("Get Stats: Should handle null values from DB as ZERO")
    void getStats_NullValues() {
        // GIVEN: DB trả về null (do không có dữ liệu)
        when(incomeRepository.sumTotalAmountByDateRange(any(), any(), any())).thenReturn(null);
        when(expenseRepository.sumTotalAmountByDateRange(any(), any(), any())).thenReturn(null);

        // WHEN
        ReportStats stats = reportService.getStats(START_DATE, END_DATE);

        // THEN
        assertEquals(BigDecimal.ZERO, stats.getTotalIncome());
        assertEquals(BigDecimal.ZERO, stats.getTotalExpense());
        assertEquals(BigDecimal.ZERO, stats.getBalance());
    }

    @Test
    @DisplayName("Get Circle Chart: Should return list from repository")
    void getExpenseCircleChart_Success() {
        List<ChartData> mockData = List.of(new ChartData("Food", BigDecimal.TEN));
        when(expenseRepository.sumAmountGroupByCategory(any(), any(), any())).thenReturn(mockData);

        List<ChartData> result = reportService.getExpenseCategoryChart(START_DATE, END_DATE);

        assertEquals(1, result.size());
        assertEquals("Food", result.get(0).getLabel());
    }

    @Test
    @DisplayName("Get History Chart: Should map raw data correctly")
    void getExpenseHistoryChart_Success() {
        // GIVEN: Raw Data [Year, Month, Amount]
        List<Object[]> rawData = new ArrayList<>();
        rawData.add(new Object[]{2025, 11, new BigDecimal("500")});

        when(expenseRepository.sumAmountGroupByMonthRaw(any(), any(), any())).thenReturn(rawData);

        // WHEN
        List<ChartData> result = reportService.getExpenseHistoryChart(START_DATE, END_DATE);

        // THEN
        assertEquals(1, result.size());
        assertEquals("11-2025", result.get(0).getLabel()); // Check logic format string
        assertEquals(new BigDecimal("500"), result.get(0).getValue());
    }
}
