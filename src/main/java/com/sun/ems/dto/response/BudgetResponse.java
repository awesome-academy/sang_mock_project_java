package com.sun.ems.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class BudgetResponse {
    private UUID id;
    private String name;
    private BigDecimal amount;
    private String period;
    
    private UUID categoryId;
    private String categoryName;
}
