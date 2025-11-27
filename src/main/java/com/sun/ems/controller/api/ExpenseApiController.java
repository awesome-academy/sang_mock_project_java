package com.sun.ems.controller.api;

import com.sun.ems.dto.request.ExpenseFilterRequest;
import com.sun.ems.dto.request.ExpenseRequest;
import com.sun.ems.dto.response.ApiResponse;
import com.sun.ems.dto.response.ExpenseDetailResponse;
import com.sun.ems.dto.response.ExpenseResponse;
import com.sun.ems.dto.response.PageResponse;
import com.sun.ems.service.api.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseApiController {

    private final ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExpenseResponse>>> getExpenses(
            @ModelAttribute @Valid ExpenseFilterRequest filterRequest) {
        
        PageResponse<ExpenseResponse> data = expenseService.getExpensesWithFilter(filterRequest);
        
        return ResponseEntity.ok(ApiResponse.<PageResponse<ExpenseResponse>>builder()
                .status(200)
                .message("Get data successfully!")
                .data(data)
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseResponse>> createExpense(@Valid @RequestBody ExpenseRequest request) {
        ExpenseResponse created = expenseService.createExpense(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ExpenseResponse>builder()
                        .status(201)
                        .message("Created successfully!")
                        .data(created)
                        .build());
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> updateExpense(
            @PathVariable UUID id, 
            @Valid @RequestBody ExpenseRequest request) {
        
        ExpenseResponse updated = expenseService.updateExpense(id, request);
        
        return ResponseEntity.ok(ApiResponse.<ExpenseResponse>builder()
                .status(200)
                .message("Updated successfully!")
                .data(updated)
                .build());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseDetailResponse>> getExpenseDetail(@PathVariable UUID id) {
        ExpenseDetailResponse detail = expenseService.getExpenseById(id);
        
        return ResponseEntity.ok(ApiResponse.<ExpenseDetailResponse>builder()
                .status(200)
                .message("Get expense detail successfully!")
                .data(detail)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(@PathVariable UUID id) {
        expenseService.deleteExpense(id);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .status(204)
                .message("Deleted successfully!")
                .data(null)
                .build());
    }
    
    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<String>>> uploadAttachments(
            @PathVariable UUID id,
            @RequestParam("files") List<MultipartFile> files) {
        
        List<String> fileUrls = expenseService.uploadAttachments(id, files);
        
        return ResponseEntity.ok(ApiResponse.<List<String>>builder()
                .status(201)
                .message("Uploaded " + files.size() + " files successfully")
                .data(fileUrls)
                .build());
    }
}
