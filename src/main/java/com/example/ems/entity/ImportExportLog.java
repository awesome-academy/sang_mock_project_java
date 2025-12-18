package com.example.ems.entity;

import com.example.ems.constant.EntityType;
import com.example.ems.constant.JobStatus;
import com.example.ems.constant.LogAction;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "import_export_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportExportLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LogAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type")
    private EntityType targetType;

    @Column(name = "file_name")
    private String fileName;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Column(columnDefinition = "TEXT")
    private String message;
}
