package com.sun.ems.service.api;

import com.sun.ems.dto.request.BudgetRequest;
import com.sun.ems.dto.response.BudgetResponse;
import com.sun.ems.entity.Budget;
import com.sun.ems.entity.Category;
import com.sun.ems.entity.User;
import com.sun.ems.exception.OperationNotPermittedException;
import com.sun.ems.exception.ResourceNotFoundException;
import com.sun.ems.repository.BudgetRepository;
import com.sun.ems.repository.CategoryRepository;
import com.sun.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgets(String period) {
        User user = getCurrentUser();
        List<Budget> budgets;

        if (period != null && !period.isEmpty()) {
            budgets = budgetRepository.findByUserIdAndPeriod(user.getId(), period);
        } else {
            budgets = budgetRepository.findByUserId(user.getId());
        }

        return budgets.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public BudgetResponse createBudget(BudgetRequest request) {
        User user = getCurrentUser();
        Category category = null;

        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            
            if (budgetRepository.existsByUserIdAndCategoryIdAndPeriod(user.getId(), category.getId(), request.getPeriod())) {
                throw new IllegalArgumentException("The budget for this category for " + request.getPeriod() + " already exists!");
            }
        } else {
            if (budgetRepository.existsByUserIdAndCategoryIsNullAndPeriod(user.getId(), request.getPeriod())) {
                throw new IllegalArgumentException("The total budget for " + request.getPeriod() + " already exists!");
            }
        }

        Budget budget = new Budget();
        budget.setName(request.getName());
        budget.setAmount(request.getAmount());
        budget.setPeriod(request.getPeriod());
        budget.setUser(user);
        budget.setCategory(category);

        return mapToResponse(budgetRepository.save(budget));
    }

    @Transactional
    public BudgetResponse updateBudget(UUID id, BudgetRequest request) {
        User user = getCurrentUser();
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        if (!budget.getUser().getId().equals(user.getId())) {
            throw new OperationNotPermittedException("You do not have permission to edit this budget");
        }

        budget.setName(request.getName());
        budget.setAmount(request.getAmount());
        
        String newPeriod = request.getPeriod();
        UUID newCategoryId = request.getCategoryId();
        
        UUID currentCategoryId = budget.getCategory() != null ? budget.getCategory().getId() : null;
        
        boolean isPeriodChanged = !budget.getPeriod().equals(newPeriod);
        boolean isCategoryChanged = (currentCategoryId == null && newCategoryId != null) || 
                                    (currentCategoryId != null && !currentCategoryId.equals(newCategoryId));

        if (isPeriodChanged || isCategoryChanged) {
            if (newCategoryId != null) {
                Category newCategory = categoryRepository.findById(newCategoryId)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
                
                if (budgetRepository.existsByUserIdAndCategoryIdAndPeriodAndIdNot(
                        user.getId(), newCategoryId, newPeriod, id)) {
                    throw new IllegalArgumentException("The budget for '" + newCategory.getName() + 
                            "' for " + newPeriod + " already exists!");
                }
                budget.setCategory(newCategory);
            } else {
                if (budgetRepository.existsByUserIdAndCategoryIsNullAndPeriodAndIdNot(
                        user.getId(), newPeriod, id)) {
                    throw new IllegalArgumentException("The total budget for " + newPeriod + " already exists!");
                }
                budget.setCategory(null);
            }
            budget.setPeriod(newPeriod);
        }
        
        return mapToResponse(budgetRepository.save(budget));
    }

    @Transactional
    public void deleteBudget(UUID id) {
        User user = getCurrentUser();
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        if (!budget.getUser().getId().equals(user.getId())) {
            throw new OperationNotPermittedException("You do not have permission to delete this budget");
        }

        budgetRepository.delete(budget);
    }

    private BudgetResponse mapToResponse(Budget budget) {
        return BudgetResponse.builder()
                .id(budget.getId())
                .name(budget.getName())
                .amount(budget.getAmount())
                .period(budget.getPeriod())
                .categoryId(budget.getCategory() != null ? budget.getCategory().getId() : null)
                .categoryName(budget.getCategory() != null ? budget.getCategory().getName() : "General Budget")
                .build();
    }
}
