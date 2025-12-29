package com.example.ems.service.admin;

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
import com.example.ems.service.ActivityLogServiceImpl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceImplTest {

    @Mock
    private ActivityLogRepository logRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ActivityLogServiceImpl activityLogService;

    private User user;
    private ActivityLog activityLog;
    private UUID logId;

    @BeforeEach
    void setUp() {
        logId = UUID.randomUUID();
        
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("admin@test.com");
        user.setName("Admin User");

        activityLog = ActivityLog.builder()
                .action(LogAction.CREATE)
                .entityType(EntityType.EXPENSE)
                .description("Test Log")
                .user(user)
                .build();
        activityLog.setId(UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- TEST RECORD ACTIVITY ---

    @Test
    @DisplayName("recordActivity - Should save log successfully with current user")
    void testRecordActivity_Success() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("adminUser"); // Not anonymous
        when(authentication.getName()).thenReturn("admin@test.com");
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));

        // When
        activityLogService.recordActivity(LogAction.UPDATE, EntityType.EXPENSE, UUID.randomUUID(), "Updated");

        // Then
        ArgumentCaptor<ActivityLog> logCaptor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(logRepository).save(logCaptor.capture());
        
        ActivityLog savedLog = logCaptor.getValue();
        assertEquals(LogAction.UPDATE, savedLog.getAction());
        assertEquals(user, savedLog.getUser());
        assertEquals("Updated", savedLog.getDescription());
    }

    @Test
    @DisplayName("recordActivity - Should handle exception gracefully (Swallow exception)")
    void testRecordActivity_Exception() {
        // Given: Repository throw exception
        doThrow(new RuntimeException("DB Error")).when(logRepository).save(any(ActivityLog.class));
        
        // Mock Security
        when(securityContext.getAuthentication()).thenReturn(null); // Case no user -> user null
        SecurityContextHolder.setContext(securityContext);

        // When & Then
        assertDoesNotThrow(() -> 
            activityLogService.recordActivity(LogAction.DELETE, EntityType.USER, UUID.randomUUID(), "Delete")
        );
    }

    // --- TEST GET LOGS (FILTER & MAPPING) ---

    @Test
    @DisplayName("getLogs - Should return mapped DTOs with User Name")
    void testGetLogs_WithUser() {
        // Given
        LogFilterRequest filter = new LogFilterRequest();
        Pageable pageable = Pageable.unpaged();
        Page<ActivityLog> page = new PageImpl<>(Collections.singletonList(activityLog));

        when(logRepository.searchLogs(any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

        // When
        Page<ActivityLogDto> result = activityLogService.getLogs(filter, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Admin User", result.getContent().get(0).getUserName()); // Verify mapToDto logic
    }

    @Test
    @DisplayName("getLogs - Should handle null User (System/Unknown)")
    void testGetLogs_NullUser() {
        // Given: 
        activityLog.setUser(null);
        Page<ActivityLog> page = new PageImpl<>(Collections.singletonList(activityLog));

        when(logRepository.searchLogs(any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

        // When
        Page<ActivityLogDto> result = activityLogService.getLogs(new LogFilterRequest(), Pageable.unpaged());

        // Then
        assertEquals("System/Unknown", result.getContent().get(0).getUserName()); // Verify null handling
    }

    // --- TEST GET LOG BY ID ---

    @Test
    @DisplayName("getLogById - Success")
    void testGetLogById_Success() {
        when(logRepository.findById(logId)).thenReturn(Optional.of(activityLog));

        ActivityLogDto dto = activityLogService.getLogById(logId);

        assertNotNull(dto);
        assertEquals(logId, dto.getId());
    }

    @Test
    @DisplayName("getLogById - Not Found")
    void testGetLogById_NotFound() {
        when(logRepository.findById(logId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> activityLogService.getLogById(logId));
    }

 // --- TEST DELETE ---

    @Test
    @DisplayName("deleteLog - Success")
    void testDeleteLog_Success() {
        // 1. Given
        when(logRepository.findById(logId)).thenReturn(Optional.of(activityLog));

        // 2. When
        activityLogService.deleteLog(logId);

        // 3. Then
        verify(logRepository).delete(activityLog);
    }

    @Test
    @DisplayName("deleteLog - Not Found")
    void testDeleteLog_NotFound() {
        // 1. Given
        when(logRepository.findById(logId)).thenReturn(Optional.empty());

        // 2. When & Then
        assertThrows(ResourceNotFoundException.class, () -> activityLogService.deleteLog(logId));
        
        verify(logRepository, never()).delete(any());
    }

    // --- TEST GET ALL USERS (SELECT DTO) ---

    @Test
    @DisplayName("getAllUsers - Should return UserSelectDto list")
    void testGetAllUsers() {
        UserSelectDto userDto = new UserSelectDto(UUID.randomUUID(), "Test Name");
        when(userRepository.findAllUserForSelect()).thenReturn(List.of(userDto));

        List<UserSelectDto> result = activityLogService.getAllUsers();

        assertFalse(result.isEmpty());
        assertEquals("Test Name", result.get(0).getName());
    }
}