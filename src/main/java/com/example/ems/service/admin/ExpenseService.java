package com.example.ems.service.admin;

import com.example.ems.dto.request.AdminExpenseDto;
import com.example.ems.dto.request.ExpenseFilterRequest;
import com.example.ems.entity.Category;
import com.example.ems.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ExpenseService {
    Page<AdminExpenseDto> getExpenses(ExpenseFilterRequest filter, UUID userId, Pageable pageable);
    AdminExpenseDto getExpenseById(UUID id);
    void saveExpense(AdminExpenseDto dto);
    void updateExpense(UUID id, AdminExpenseDto dto);
    void deleteExpense(UUID id);
    
    List<User> getAllUsers();
    List<Category> getIncomeCategories();
}
