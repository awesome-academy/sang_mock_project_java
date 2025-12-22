package com.example.ems.service.admin;

import com.example.ems.dto.request.AdminIncomeDto;
import com.example.ems.dto.request.IncomeFilterRequest;
import com.example.ems.entity.Category;
import com.example.ems.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface IncomeService {
    Page<AdminIncomeDto> getIncomes(IncomeFilterRequest filter, Pageable pageable);
    AdminIncomeDto getIncomeById(UUID id);
    void saveIncome(AdminIncomeDto dto);
    void updateIncome(UUID id, AdminIncomeDto dto);
    void deleteIncome(UUID id);
    List<User> getAllUsers();
    List<Category> getIncomeCategories();
}
