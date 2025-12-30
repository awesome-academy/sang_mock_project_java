package com.example.ems.service.csv;

import com.example.ems.constant.EntityType;
import com.example.ems.constant.JobStatus;
import com.example.ems.constant.LogAction;
import com.example.ems.dto.request.ImportLogFilterRequest;
import com.example.ems.entity.ImportExportLog;
import com.example.ems.entity.User;
import com.example.ems.exception.ResourceNotFoundException;
import com.example.ems.repository.ImportExportLogRepository;
import com.example.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImportExportLogService {

    private final ImportExportLogRepository logRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        
        String email = authentication.getName();
        
        return userRepository.findByEmail(email).orElse(null);
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID startLog(LogAction action, EntityType targetType, String fileName) {
        User currentUser = getCurrentUser();

        if (currentUser == null) {
            throw new ResourceNotFoundException("User not found");
        }

        ImportExportLog importLog = ImportExportLog.builder()
                .user(currentUser)
                .action(action)
                .targetType(targetType)
                .fileName(fileName)
                .status(JobStatus.IN_PROGRESS)
                .message("Processing started...")
                .build();

        return logRepository.save(importLog).getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finishLogSuccess(UUID logId, String message) {
        logRepository.findById(logId).ifPresent(log -> {
            log.setStatus(JobStatus.SUCCESS);
            log.setMessage(message);
            logRepository.save(log);
        });
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finishLogFailed(UUID logId, String errorMessage) {
        logRepository.findById(logId).ifPresent(log -> {
            log.setStatus(JobStatus.FAILED);
            log.setMessage(errorMessage);
            logRepository.save(log);
        });
    }
    
    public Page<ImportExportLog> getLogs(ImportLogFilterRequest filter, Pageable pageable) {
        LocalDateTime startDateTime = (filter.getStartDate() != null) ? filter.getStartDate().atStartOfDay() : null;
        LocalDateTime endDateTime = (filter.getEndDate() != null) ? filter.getEndDate().atTime(LocalTime.MAX) : null;

        return logRepository.searchLogs(
                startDateTime,
                endDateTime,
                filter.getStatus(),
                filter.getType(),
                filter.getTargetType(),
                pageable
        );
    }
}
