package com.example.ems.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ExpenseResponse {
    private UUID id;
    private String title;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String note;
    
    private UUID categoryId;
    private String categoryName;
    private String categoryIcon;

    private String budgetAlert;
    private List<String> attachments;
}
