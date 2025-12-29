package com.example.ems.dto.request;

import com.example.ems.constant.EntityType;
import com.example.ems.constant.LogAction;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class LogFilterRequest {
    private Integer page;
    private Integer size;
    
    private String keyword;
    private UUID userId;
    private LogAction action;
    private EntityType entityType;
    
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
