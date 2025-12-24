package com.example.ems.repository;

import com.example.ems.constant.EntityType;
import com.example.ems.constant.LogAction;
import com.example.ems.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT l FROM ActivityLog l " +
           "WHERE (:userId IS NULL OR l.user.id = :userId) " +
           "AND (:action IS NULL OR l.action = :action) " +
           "AND (:entityType IS NULL OR l.entityType = :entityType) " +
           "AND (:startDate IS NULL OR function('date', l.createdAt) >= :startDate) " +
           "AND (:endDate IS NULL OR function('date', l.createdAt) <= :endDate) " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(l.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ActivityLog> searchLogs(
            @Param("userId") UUID userId,
            @Param("action") LogAction action,
            @Param("entityType") EntityType entityType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("keyword") String keyword,
            Pageable pageable);
}
