package com.example.ems.repository;

import com.example.ems.constant.EntityType;
import com.example.ems.constant.JobStatus;
import com.example.ems.constant.LogAction;
import com.example.ems.entity.ImportExportLog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface ImportExportLogRepository extends JpaRepository<ImportExportLog, UUID> {
	@Query("SELECT l FROM ImportExportLog l WHERE " +
           "(:startDate IS NULL OR l.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR l.createdAt <= :endDate) AND " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:action IS NULL OR l.action = :action) AND " +
           "(:targetType IS NULL OR l.targetType = :targetType)")
    Page<ImportExportLog> searchLogs(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") JobStatus status,
            @Param("action") LogAction action,
            @Param("targetType") EntityType targetType,
            Pageable pageable
    );
}
