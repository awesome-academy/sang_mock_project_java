
package com.example.ems.dto.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class IncomeFilterRequest {
    private Integer page;
    private Integer size;
    private String keyword;
    private UUID categoryId;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    public int getPage() {
        return (page == null || page <= 0) ? 1 : page;
    }

    public int getSize() {
        return (size == null || size <= 0) ? 10 : size;
    }
}
