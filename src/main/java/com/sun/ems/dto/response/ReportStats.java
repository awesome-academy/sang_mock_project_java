package com.sun.ems.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ReportStats {
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal balance;
}
