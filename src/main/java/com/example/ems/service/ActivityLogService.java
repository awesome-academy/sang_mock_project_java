package com.example.ems.service;

import com.example.ems.constant.EntityType;
import com.example.ems.constant.LogAction;
import com.example.ems.dto.request.LogFilterRequest;
import com.example.ems.dto.response.ActivityLogDto;
import com.example.ems.dto.response.UserSelectDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ActivityLogService {
    void recordActivity(LogAction action, EntityType entityType, UUID entityId, String description);
    
    Page<ActivityLogDto> getLogs(LogFilterRequest filter, Pageable pageable);
    
    void deleteLog(UUID id);
    
    List<UserSelectDto> getAllUsers();
    
    ActivityLogDto getLogById(UUID id);
}
