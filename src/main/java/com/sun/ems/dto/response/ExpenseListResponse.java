package com.sun.ems.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ExpenseListResponse {
    private List<ExpenseResponse> items;
    
    private List<String> globalAlerts; 
}
