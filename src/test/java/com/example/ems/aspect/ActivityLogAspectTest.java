package com.example.ems.aspect;

import com.example.ems.annotation.LogActivity;
import com.example.ems.constant.EntityType;
import com.example.ems.constant.LogAction;
import com.example.ems.service.ActivityLogService;
import com.example.ems.util.ObjectDiffer;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityLogAspectTest {

    @Mock
    private ActivityLogService logService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ObjectDiffer objectDiffer;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private LogActivity logActivity;

    @InjectMocks
    private ActivityLogAspect activityLogAspect;

    private UUID entityId;
    private TestEntity oldEntity;
    private TestEntity newEntity;

    static class TestEntity {
        private UUID id;
        private String name;

        public TestEntity(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @BeforeEach
    void setUp() {
        entityId = UUID.randomUUID();
        oldEntity = new TestEntity(entityId, "Old Name");
        newEntity = new TestEntity(entityId, "New Name");
    }

    @Test
    @DisplayName("Test Log CREATE: Should extract ID from return value")
    void testLogCreate_Success() throws Throwable {
        // Given
        when(logActivity.action()).thenReturn(LogAction.CREATE);
        when(logActivity.entityType()).thenReturn(EntityType.EXPENSE);
        
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        
        when(joinPoint.proceed()).thenReturn(newEntity);

        // When
        activityLogAspect.logActivity(joinPoint, logActivity);

        // Then
        verify(logService).recordActivity(
                eq(LogAction.CREATE),
                eq(EntityType.EXPENSE),
                eq(entityId),
                contains("Created new EXPENSE")
        );
    }

    @Test
    @DisplayName("Test Log DELETE: Should extract ID from arguments")
    void testLogDelete_Success() throws Throwable {
        // Given
        when(logActivity.action()).thenReturn(LogAction.DELETE);
        when(logActivity.entityType()).thenReturn(EntityType.EXPENSE);
        
        when(joinPoint.getArgs()).thenReturn(new Object[]{entityId});

        // When
        activityLogAspect.logActivity(joinPoint, logActivity);

        // Then
        verify(logService).recordActivity(
                eq(LogAction.DELETE),
                eq(EntityType.EXPENSE),
                eq(entityId),
                contains("Deleted EXPENSE")
        );
    }

    @Test
    @DisplayName("Test Log UPDATE: Should log changes when diff exists")
    void testLogUpdate_WithChanges() throws Throwable {
        // Given
        when(logActivity.action()).thenReturn(LogAction.UPDATE);
        when(logActivity.entityType()).thenReturn(EntityType.EXPENSE);
        doReturn(TestEntity.class).when(logActivity).entityClass(); // Mock class type

        when(joinPoint.getArgs()).thenReturn(new Object[]{entityId});

        // 1. Pre-execution: 
        when(entityManager.find(TestEntity.class, entityId))
                .thenReturn(oldEntity)
                .thenReturn(newEntity);

        // 2. Logic Diff
        when(objectDiffer.diff(oldEntity, newEntity)).thenReturn("Name: 'Old Name' -> 'New Name'");

        // When
        activityLogAspect.logActivity(joinPoint, logActivity);

        // Then
        verify(entityManager).detach(oldEntity);
        verify(logService).recordActivity(
                eq(LogAction.UPDATE),
                eq(EntityType.EXPENSE),
                eq(entityId),
                eq("Updated: Name: 'Old Name' -> 'New Name'")
        );
    }

    @Test
    @DisplayName("Test Log UPDATE: Should NOT log if no changes")
    void testLogUpdate_NoChanges() throws Throwable {
        // Given
        when(logActivity.action()).thenReturn(LogAction.UPDATE);
        doReturn(TestEntity.class).when(logActivity).entityClass();
        when(joinPoint.getArgs()).thenReturn(new Object[]{entityId});

        when(entityManager.find(TestEntity.class, entityId)).thenReturn(oldEntity);
        when(objectDiffer.diff(any(), any())).thenReturn("");

        // When
        activityLogAspect.logActivity(joinPoint, logActivity);

        // Then
        verify(logService, never()).recordActivity(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Test Exception Handling: Should swallow exception during logging")
    void testExceptionHandling() throws Throwable {
        // Given
        when(logActivity.action()).thenReturn(LogAction.DELETE);
        
        when(logActivity.entityType()).thenReturn(EntityType.EXPENSE); 
        
        when(joinPoint.getArgs()).thenReturn(new Object[]{entityId});

        doThrow(new RuntimeException("DB Error")).when(logService)
                .recordActivity(any(), any(), any(), any());

        // When & Then
        activityLogAspect.logActivity(joinPoint, logActivity);
        
        verify(joinPoint).proceed();
    }
}
