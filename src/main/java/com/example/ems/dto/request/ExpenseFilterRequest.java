
package com.example.ems.dto.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class ExpenseFilterRequest {
	@Min(value = 1, message = "Page must be at least 1")
    private Integer page;
	
	@Min(value = 1, message = "Size must be at least 1")
	@Max(value = 50, message = "Size cannot exceed 50")
    private Integer size;
    
    private String keyword;
    private UUID categoryId;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    
    public int getPage() {
        if (page == null || page <= 0) {
            return 1;
        }
        return page;
    }

    public int getSize() {
        if (size == null || size <= 0) {
            return 10;
        }
        return size;
    }
}
