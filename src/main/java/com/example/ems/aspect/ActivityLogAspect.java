package com.example.ems.aspect;

import com.example.ems.annotation.LogActivity;
import com.example.ems.constant.LogAction;
import com.example.ems.service.ActivityLogService;
import com.example.ems.util.ObjectDiffer;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field; // Import Reflection
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityLogAspect {

    private final ActivityLogService logService;
    private final EntityManager entityManager;
    private final ObjectDiffer objectDiffer;

    @Around("@annotation(logActivity)")
    public Object logActivity(ProceedingJoinPoint joinPoint, LogActivity logActivity) throws Throwable {
        LogAction action = logActivity.action();
        Object oldEntity = null;
        UUID entityId = null;

        if (action == LogAction.UPDATE || action == LogAction.DELETE) {
            entityId = getEntityIdFromArgs(joinPoint);
            if (entityId != null && action == LogAction.UPDATE) {
                oldEntity = entityManager.find(logActivity.entityClass(), entityId);
                if (oldEntity != null) {
                    entityManager.detach(oldEntity);
                }
            }
        }

        Object result = joinPoint.proceed();

        try {
            if (entityId == null) {
                entityId = getEntityIdFromArgs(joinPoint);
                
                if (entityId == null && result != null) {
                    entityId = getIdFromObject(result);
                }
            }

            String description = "";

            switch (action) {
                case CREATE:
                    description = "Created new " + logActivity.entityType().name();
                    break;
                case DELETE:
                    description = "Deleted " + logActivity.entityType().name();
                    break;
                case UPDATE:
                    if (oldEntity != null && entityId != null) {
                        Object newEntity = entityManager.find(logActivity.entityClass(), entityId);
                        String diff = objectDiffer.diff(oldEntity, newEntity);
                        if (diff.isEmpty()) return result;
                        description = "Updated: " + diff;
                    } else {
                        description = "Updated " + logActivity.entityType().name();
                    }
                    break;
                default:
                    description = action.name() + " " + logActivity.entityType().name();
            }

            logService.recordActivity(action, logActivity.entityType(), entityId, description);

        } catch (Exception e) {
            log.error("Error logging activity aspect: {}", e.getMessage());
        }

        return result;
    }

    private UUID getEntityIdFromArgs(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof UUID) {
            return (UUID) args[0];
        }
        return null;
    }

    private UUID getIdFromObject(Object obj) {
        try {
            Field field = obj.getClass().getDeclaredField("id");
            field.setAccessible(true);
            Object value = field.get(obj);
            if (value instanceof UUID) {
                return (UUID) value;
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
        }
        
        return null;
    }
}
