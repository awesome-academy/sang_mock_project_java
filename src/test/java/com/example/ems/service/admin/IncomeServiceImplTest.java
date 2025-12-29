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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncomeServiceImplTest {

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private IncomeServiceImpl incomeService;

    private Income income;
    private AdminIncomeDto incomeDto;
    private User user;
    private Category category;
    private UUID incomeId;
    private UUID userId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        incomeId = UUID.randomUUID();
        userId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        // Setup Entities
        user = new User();
        user.setId(userId);
        user.setName("Test User");

        category = new Category();
        category.setId(categoryId);
        category.setName("Test Category");

        income = new Income();
        income.setId(incomeId);
        income.setTitle("Test Income");
        income.setAmount(BigDecimal.valueOf(1000));
        income.setIncomeDate(LocalDate.now());
        income.setUser(user);
        income.setCategory(category);

        // Setup DTO
        incomeDto = new AdminIncomeDto();
        incomeDto.setTitle("Test Income");
        incomeDto.setAmount(BigDecimal.valueOf(1000));
        incomeDto.setIncomeDate(LocalDate.now());
        incomeDto.setUserId(userId);
        incomeDto.setCategoryId(categoryId);
    }

    // --- TEST SEARCH ---

    @Test
    @DisplayName("getIncomes - Should return page of mapped DTOs")
    void testGetIncomes() {
        IncomeFilterRequest filter = new IncomeFilterRequest();
        Pageable pageable = Pageable.unpaged();
        Page<Income> incomePage = new PageImpl<>(Collections.singletonList(income));

        when(incomeRepository.searchIncomes(any(), any(), any(), any(), any(), any()))
                .thenReturn(incomePage);

        Page<AdminIncomeDto> result = incomeService.getIncomes(filter, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(income.getTitle(), result.getContent().get(0).getTitle());
        verify(incomeRepository).searchIncomes(any(), any(), any(), any(), any(), any());
    }

    // --- TEST GET BY ID ---

    @Test
    @DisplayName("getIncomeById - Success")
    void testGetIncomeById_Success() {
        when(incomeRepository.findById(incomeId)).thenReturn(Optional.of(income));

        AdminIncomeDto result = incomeService.getIncomeById(incomeId);

        assertNotNull(result);
        assertEquals(incomeId, result.getId());
    }

    @Test
    @DisplayName("getIncomeById - Not Found")
    void testGetIncomeById_NotFound() {
        when(incomeRepository.findById(incomeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> incomeService.getIncomeById(incomeId));
    }

    // --- TEST SAVE ---

    @Test
    @DisplayName("saveIncome - Success")
    void testSaveIncome_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        incomeService.saveIncome(incomeDto);

        verify(incomeRepository).save(any(Income.class));
    }


    @Test
    @DisplayName("updateIncome - Success")
    void testUpdateIncome_Success() {
        // Given: Find thấy income
        when(incomeRepository.findById(incomeId)).thenReturn(Optional.of(income));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        // When
        incomeService.updateIncome(incomeId, incomeDto);

        // Then
        verify(incomeRepository).save(income);
        assertEquals(incomeDto.getTitle(), income.getTitle()); // Verify data mapping
    }

    @Test
    @DisplayName("updateIncome - Not Found")
    void testUpdateIncome_NotFound() {
        // Given: Không tìm thấy income
        when(incomeRepository.findById(incomeId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> incomeService.updateIncome(incomeId, incomeDto));
        verify(incomeRepository, never()).save(any(Income.class));
    }

    // --- TEST DELETE 

    @Test
    @DisplayName("deleteIncome - Success")
    void testDeleteIncome_Success() {
        // Given
        when(incomeRepository.findById(incomeId)).thenReturn(Optional.of(income));

        // When
        incomeService.deleteIncome(incomeId);

        // Then
        verify(incomeRepository).delete(income);
    }

    @Test
    @DisplayName("deleteIncome - Not Found")
    void testDeleteIncome_NotFound() {
        // Given
        when(incomeRepository.findById(incomeId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> incomeService.deleteIncome(incomeId));
        
        // Verify
        verify(incomeRepository, never()).delete(any(Income.class));
        verify(incomeRepository, never()).deleteById(any(UUID.class));
    }

    // --- TEST HELPER METHODS ---

    @Test
    @DisplayName("getAllUsers - Success")
    void testGetAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<User> result = incomeService.getAllUsers();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getIncomeCategories - Success")
    void testGetIncomeCategories() {
        Page<Category> categoryPage = new PageImpl<>(List.of(category));
        when(categoryRepository.searchGlobalCategories(anyString(), eq(CategoryType.INCOME), any(Pageable.class)))
                .thenReturn(categoryPage);

        List<Category> result = incomeService.getIncomeCategories();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }
}