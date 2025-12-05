package com.sun.ems.controller.api;

import com.sun.ems.dto.response.ApiResponse;
import com.sun.ems.dto.response.ChartData;
import com.sun.ems.dto.response.ReportStats;
import com.sun.ems.service.api.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportApiController {

    private final ReportService reportService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ReportStats>> getStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        ReportStats stats = reportService.getStats(startDate, endDate);
        return ResponseEntity.ok(new ApiResponse<>(200, "Get stats successfully", stats));
    }

    @GetMapping("/chart/expense-category")
    public ResponseEntity<ApiResponse<List<ChartData>>> getExpenseCircleChart(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<ChartData> data = reportService.getExpenseCategoryChart(startDate, endDate);
        return ResponseEntity.ok(new ApiResponse<>(200, "Get chart data successfully", data));
    }

    @GetMapping("/chart/expense-history")
    public ResponseEntity<ApiResponse<List<ChartData>>> getExpenseHistoryChart(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<ChartData> data = reportService.getExpenseHistoryChart(startDate, endDate);
        return ResponseEntity.ok(new ApiResponse<>(200, "Get chart data successfully", data));
    }
}