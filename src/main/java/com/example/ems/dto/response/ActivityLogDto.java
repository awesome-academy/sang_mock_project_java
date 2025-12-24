package com.example.ems.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ActivityLogDto {
    private UUID id;
    private LocalDateTime timestamp;
    private String action; 
    private String entityType;
    private String description;
    private String userName;
    private UUID entityId;
}
