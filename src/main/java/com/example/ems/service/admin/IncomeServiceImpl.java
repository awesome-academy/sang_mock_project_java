package com.example.ems.service.admin;

import com.example.ems.constant.CategoryType;
import com.example.ems.dto.request.AdminIncomeDto;
import com.example.ems.dto.request.IncomeFilterRequest;
import com.example.ems.entity.Category;
import com.example.ems.entity.Income;
import com.example.ems.entity.User;
import com.example.ems.exception.ResourceNotFoundException;
import com.example.ems.repository.CategoryRepository;
import com.example.ems.repository.IncomeRepository;
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
public class IncomeServiceImpl implements IncomeService {

    private final IncomeRepository incomeRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public Page<AdminIncomeDto> getIncomes(IncomeFilterRequest filter, Pageable pageable) {
        return incomeRepository.searchIncomes(
        		filter.getUserId(),
                filter.getCategoryId(),
                filter.getStartDate(),
                filter.getEndDate(),
                filter.getKeyword(),
                pageable
        ).map(this::mapToDto);
    }

    @Override
    public AdminIncomeDto getIncomeById(UUID id) {
        Income income = incomeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Income not found"));
        return mapToDto(income);
    }

    @Override
    @Transactional
    public void saveIncome(AdminIncomeDto dto) {
        Income income = new Income();
        mapToEntity(dto, income);
        incomeRepository.save(income);
    }

    @Override
    @Transactional
    public void updateIncome(UUID id, AdminIncomeDto dto) {
    	incomeRepository.findById(id).ifPresentOrElse(
            income -> {
                mapToEntity(dto, income);
                incomeRepository.save(income);
            },
            () -> {
                throw new ResourceNotFoundException("Income not found");
            }
        );
    }

    @Override
    @Transactional
    public void deleteIncome(UUID id) {
    	incomeRepository.findById(id).ifPresentOrElse(
            incomeRepository::delete,
            () -> {
                throw new ResourceNotFoundException("Income not found");
            }
        );
    }
    
    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public List<Category> getIncomeCategories() {
        Pageable dropdownLimit = PageRequest.of(0, 100, Sort.by("name"));
        return categoryRepository.searchGlobalCategories("", CategoryType.INCOME, dropdownLimit).getContent();
    }
    

    private void mapToEntity(AdminIncomeDto dto, Income entity) {
        entity.setTitle(dto.getTitle());
        entity.setAmount(dto.getAmount());
        entity.setIncomeDate(dto.getIncomeDate());
        entity.setNote(dto.getNote());

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        entity.setUser(user);

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        entity.setCategory(category);
    }

    private AdminIncomeDto mapToDto(Income entity) {
        AdminIncomeDto dto = new AdminIncomeDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setAmount(entity.getAmount());
        dto.setIncomeDate(entity.getIncomeDate());
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