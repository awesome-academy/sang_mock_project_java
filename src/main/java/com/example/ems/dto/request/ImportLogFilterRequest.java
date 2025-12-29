package com.example.ems.dto.request;

import com.example.ems.constant.JobStatus;
import com.example.ems.constant.LogAction;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class ImportLogFilterRequest {
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    private JobStatus status;
    private LogAction type;
}
