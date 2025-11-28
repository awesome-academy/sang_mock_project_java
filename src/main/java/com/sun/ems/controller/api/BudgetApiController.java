package com.sun.ems.controller.api;

import com.sun.ems.dto.request.BudgetRequest;
import com.sun.ems.dto.response.ApiResponse;
import com.sun.ems.dto.response.BudgetResponse;
import com.sun.ems.service.api.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetApiController {

    private final BudgetService budgetService;

    // GET /api/budgets?period=11-2025
    @GetMapping
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getBudgets(
            @RequestParam(required = false) String period) {
        List<BudgetResponse> budgets = budgetService.getBudgets(period);
        
        return ResponseEntity.ok(ApiResponse.<List<BudgetResponse>>builder()
			.status(200)
			.message("Success")
			.data(budgets)
			.build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponse>> createBudget(@Valid @RequestBody BudgetRequest request) {
        BudgetResponse created = budgetService.createBudget(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.<BudgetResponse>builder()
			.status(201)
			.message("Budget created successfully")
			.data(created)
			.build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetResponse>> updateBudget(
            @PathVariable UUID id,
            @Valid @RequestBody BudgetRequest request) {
        BudgetResponse updated = budgetService.updateBudget(id, request);
        
        return ResponseEntity.ok(ApiResponse.<BudgetResponse>builder()
			.status(200)
			.message("Budget updated successfully")
			.data(updated)
			.build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(@PathVariable UUID id) {
        budgetService.deleteBudget(id);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
			    .status(200)
			    .message("Budget deleted successfully")
			    .data(null)
			    .build());
    }
}
