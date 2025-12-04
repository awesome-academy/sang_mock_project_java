package com.sun.ems.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class IncomeResponse {
    private UUID id;
    private String title;
    private BigDecimal amount;
    private LocalDate incomeDate;
    private String note;
    
    // Category Info
    private UUID categoryId;
    private String categoryName;
    private String categoryIcon;
}
