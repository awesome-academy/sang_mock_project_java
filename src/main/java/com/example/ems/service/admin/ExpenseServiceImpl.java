package com.example.ems.service.admin;

import com.example.ems.constant.CategoryType;
import com.example.ems.dto.request.AdminExpenseDto;
import com.example.ems.dto.request.ExpenseFilterRequest;
import com.example.ems.entity.Category;
import com.example.ems.entity.Expense;
import com.example.ems.entity.User;
import com.example.ems.exception.ResourceNotFoundException;
import com.example.ems.repository.CategoryRepository;
import com.example.ems.repository.ExpenseRepository;
import com.example.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public Page<AdminExpenseDto> getExpenses(ExpenseFilterRequest filter, UUID userId, Pageable pageable) {
        return expenseRepository.searchExpenses(
                userId,
                filter.getCategoryId(),
                filter.getStartDate(),
                filter.getEndDate(),
                filter.getKeyword(),
                pageable
        ).map(this::mapToDto);
    }

    @Override
    public AdminExpenseDto getExpenseById(UUID id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        return mapToDto(expense);
    }

    @Override
    @Transactional
    public void saveExpense(AdminExpenseDto dto) {
        Expense expense = new Expense();
        mapToEntity(dto, expense);
        expenseRepository.save(expense);
    }

    @Override
    @Transactional
    public void updateExpense(UUID id, AdminExpenseDto dto) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        mapToEntity(dto, expense);
        expenseRepository.save(expense);
    }

    @Override
    @Transactional
    public void deleteExpense(UUID id) {
        expenseRepository.deleteById(id);
    }
    
    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public List<Category> getIncomeCategories() {
        Pageable dropdownLimit = PageRequest.of(0, 100, Sort.by("name"));
        return categoryRepository.searchGlobalCategories("", CategoryType.EXPENSE, dropdownLimit).getContent();
    }

    private void mapToEntity(AdminExpenseDto dto, Expense entity) {
        entity.setTitle(dto.getTitle());
        entity.setAmount(dto.getAmount());
        entity.setExpenseDate(dto.getExpenseDate());
        entity.setNote(dto.getNote());

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        entity.setUser(user);

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        entity.setCategory(category);
    }

    private AdminExpenseDto mapToDto(Expense entity) {
        AdminExpenseDto dto = new AdminExpenseDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setAmount(entity.getAmount());
        dto.setExpenseDate(entity.getExpenseDate());
        dto.setNote(entity.getNote());
        if (entity.getUser() != null) {
            dto.setUserId(entity.getUser().getId());
            dto.setUserName(entity.getUser().getName());
        }
        
        if (entity.getCategory() != null) {
            dto.setCategoryId(entity.getCategory().getId());
            dto.setCategoryName(entity.getCategory().getName());
        }
        return dto;
    }
}
