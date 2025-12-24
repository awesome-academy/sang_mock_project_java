package com.example.ems.service;

import com.example.ems.constant.EntityType;
import com.example.ems.constant.LogAction;
import com.example.ems.dto.request.LogFilterRequest;
import com.example.ems.dto.response.ActivityLogDto;
import com.example.ems.dto.response.UserSelectDto;
import com.example.ems.entity.ActivityLog;
import com.example.ems.entity.User;
import com.example.ems.exception.ResourceNotFoundException;
import com.example.ems.repository.ActivityLogRepository;
import com.example.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository logRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void recordActivity(LogAction action, EntityType entityType, UUID entityId, String description) {
        try {
            User currentUser = getCurrentUser();
            
            ActivityLog newLog = ActivityLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .description(description)
                    .user(currentUser)
                    .build();
            
            logRepository.save(newLog);
        } catch (Exception e) {
            log.error("Failed to record activity log", e);
        }
    }

    @Override
    public Page<ActivityLogDto> getLogs(LogFilterRequest filter, Pageable pageable) {
        return logRepository.searchLogs(
                filter.getUserId(),
                filter.getAction(),
                filter.getEntityType(),
                filter.getStartDate(),
                filter.getEndDate(),
                filter.getKeyword(),
                pageable
        ).map(this::mapToDto);
    }

    @Override
    @Transactional
    public void deleteLog(UUID id) {
    	ActivityLog log = logRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity Log not found with id: " + id));
        logRepository.delete(log);
    }
    
    @Override
    public List<UserSelectDto> getAllUsers() {
        return userRepository.findAllUserForSelect();
    }
    
    @Override
    @Transactional(readOnly = true)
    public ActivityLogDto getLogById(UUID id) {
        ActivityLog log = logRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity Log not found with id: " + id));
        return mapToDto(log);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email).orElse(null);
    }

    private ActivityLogDto mapToDto(ActivityLog entity) {
        ActivityLogDto dto = new ActivityLogDto();
        dto.setId(entity.getId());
        dto.setTimestamp(entity.getCreatedAt()); // BaseEntity
        dto.setAction(entity.getAction().name());
        dto.setEntityType(entity.getEntityType().name());
        dto.setDescription(entity.getDescription());
        dto.setEntityId(entity.getEntityId());
        
        if (entity.getUser() != null) {
            dto.setUserName(entity.getUser().getName());
        } else {
            dto.setUserName("System/Unknown");
        }
        return dto;
    }
}