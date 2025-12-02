package com.sun.ems.service.api;

import com.sun.ems.constant.CategoryType;
import com.sun.ems.dto.request.IncomeFilterRequest;
import com.sun.ems.dto.request.IncomeRequest;
import com.sun.ems.dto.response.IncomeResponse;
import com.sun.ems.dto.response.PageResponse;
import com.sun.ems.entity.Category;
import com.sun.ems.entity.Income;
import com.sun.ems.entity.User;
import com.sun.ems.exception.OperationNotPermittedException;
import com.sun.ems.exception.ResourceNotFoundException;
import com.sun.ems.repository.CategoryRepository;
import com.sun.ems.repository.IncomeRepository;
import com.sun.ems.repository.IncomeSpecification;
import com.sun.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    // 1. Get List with Filter & Pagination
    @Transactional(readOnly = true)
    public PageResponse<IncomeResponse> getIncomes(IncomeFilterRequest filter) {
        User user = getCurrentUser();

        int pageNo = filter.getPage() < 1 ? 0 : filter.getPage() - 1;
        Pageable pageable = PageRequest.of(pageNo, filter.getSize());

        Page<Income> pageResult = incomeRepository.findAll(
                IncomeSpecification.getFilter(user.getId(), filter),
                pageable
        );

        List<IncomeResponse> content = pageResult.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<IncomeResponse>builder()
                .content(content)
                .pageNo(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    // 2. Create Income
    @Transactional
    public IncomeResponse createIncome(IncomeRequest request) {
        User user = getCurrentUser();

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (category.getType() != CategoryType.INCOME) {
            throw new IllegalArgumentException("Selected category is not an Income category");
        }

        Income income = new Income();
        income.setTitle(request.getTitle());
        income.setAmount(request.getAmount());
        income.setIncomeDate(request.getIncomeDateAsLocalDate()); // Helper convert String->Date
        income.setNote(request.getNote());
        income.setCategory(category);
        income.setUser(user);

        return mapToResponse(incomeRepository.save(income));
    }

    // 3. Update Income
    @Transactional
    public IncomeResponse updateIncome(UUID id, IncomeRequest request) {
        User user = getCurrentUser();

        Income income = incomeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Income not found"));

        if (!income.getUser().getId().equals(user.getId())) {
            throw new OperationNotPermittedException("You do not have permission to edit this income");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (category.getType() != CategoryType.INCOME) {
            throw new IllegalArgumentException("Selected category is not an Income category");
        }

        income.setTitle(request.getTitle());
        income.setAmount(request.getAmount());
        income.setIncomeDate(request.getIncomeDateAsLocalDate());
        income.setNote(request.getNote());
        income.setCategory(category);

        return mapToResponse(incomeRepository.save(income));
    }

    // 4. Delete Income
    @Transactional
    public void deleteIncome(UUID id) {
        User user = getCurrentUser();
        Income income = incomeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Income not found"));

        if (!income.getUser().getId().equals(user.getId())) {
            throw new OperationNotPermittedException("You do not have permission to delete this income");
        }

        incomeRepository.delete(income);
    }

    // 5. Get Detail
    @Transactional(readOnly = true)
    public IncomeResponse getIncomeById(UUID id) {
        User user = getCurrentUser();
        Income income = incomeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Income not found"));

        if (!income.getUser().getId().equals(user.getId())) {
            throw new OperationNotPermittedException("You do not have permission to view this income");
        }

        return mapToResponse(income);
    }

    private IncomeResponse mapToResponse(Income income) {
        return IncomeResponse.builder()
                .id(income.getId())
                .title(income.getTitle())
                .amount(income.getAmount())
                .incomeDate(income.getIncomeDate())
                .note(income.getNote())
                .categoryId(income.getCategory().getId())
                .categoryName(income.getCategory().getName())
                .categoryIcon(income.getCategory().getIcon())
                .build();
    }
}
