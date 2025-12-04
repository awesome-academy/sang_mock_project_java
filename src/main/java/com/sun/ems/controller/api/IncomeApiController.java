package com.sun.ems.controller.api;

import com.sun.ems.dto.request.IncomeFilterRequest;
import com.sun.ems.dto.request.IncomeRequest;
import com.sun.ems.dto.response.ApiResponse;
import com.sun.ems.dto.response.IncomeResponse;
import com.sun.ems.dto.response.PageResponse;
import com.sun.ems.service.api.IncomeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/incomes")
@RequiredArgsConstructor
public class IncomeApiController {

    private final IncomeService incomeService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<IncomeResponse>>> getIncomes(
            @ModelAttribute IncomeFilterRequest filter) {
        
        PageResponse<IncomeResponse> data = incomeService.getIncomes(filter);
        
        return ResponseEntity.ok(ApiResponse.<PageResponse<IncomeResponse>>builder()
                .status(200)
                .message("Get data successfully!")
                .data(data)
                .build());    }

    @PostMapping
    public ResponseEntity<ApiResponse<IncomeResponse>> createIncome(
            @Valid @RequestBody IncomeRequest request) {
        
        IncomeResponse created = incomeService.createIncome(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<IncomeResponse>builder()
                        .status(201)
                        .message("Created successfully!")
                        .data(created)
                        .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<IncomeResponse>> updateIncome(
            @PathVariable UUID id,
            @Valid @RequestBody IncomeRequest request) {
        
        IncomeResponse updated = incomeService.updateIncome(id, request);
        
        return ResponseEntity.ok(ApiResponse.<IncomeResponse>builder()
                .status(200)
                .message("Updated successfully!")
                .data(updated)
                .build());    
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IncomeResponse>> getIncomeDetail(@PathVariable UUID id) {
        IncomeResponse detail = incomeService.getIncomeById(id);
        
        return ResponseEntity.ok(ApiResponse.<IncomeResponse>builder()
                .status(200)
                .message("Get income detail successfully!")
                .data(detail)
                .build());    
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteIncome(@PathVariable UUID id) {
        incomeService.deleteIncome(id);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .status(204)
                .message("Deleted successfully!")
                .data(null)
                .build());    
    }
}
