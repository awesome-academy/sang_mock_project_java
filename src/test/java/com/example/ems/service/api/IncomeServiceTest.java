
package com.example.ems.service.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.ems.constant.CategoryType;
import com.example.ems.dto.request.IncomeRequest;
import com.example.ems.dto.response.IncomeResponse;
import com.example.ems.entity.Category;
import com.example.ems.entity.Income;
import com.example.ems.entity.User;
import com.example.ems.exception.OperationNotPermittedException;
import com.example.ems.exception.ResourceNotFoundException;
import com.example.ems.repository.CategoryRepository;
import com.example.ems.repository.IncomeRepository;
import com.example.ems.repository.UserRepository;
import com.example.ems.service.api.IncomeService;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncomeServiceTest {

    @Mock private IncomeRepository incomeRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private IncomeService incomeService;

    private User mockUser;
    private Category incomeCategory;
    private final String TEST_EMAIL = "test@email.com";

    @BeforeEach
    void setUp() {
        // Mock Security Context
        Authentication auth = mock(Authentication.class);
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        
        // Mock User
        mockUser = User.builder().name("Test").email(TEST_EMAIL).build();
        mockUser.setId(UUID.randomUUID());

        // Mock Category (INCOME type)
        incomeCategory = Category.builder().name("Salary").type(CategoryType.INCOME).user(mockUser).build();
        incomeCategory.setId(UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- CREATE TESTS ---
    @Test
    @DisplayName("Create: Should succeed with valid data")
    void createIncome_Success() {
        // GIVEN
        IncomeRequest req = new IncomeRequest();
        req.setTitle("Salary");
        req.setAmount(new BigDecimal("1000"));
        req.setIncomeDate("2025-12-01"); // String format
        req.setCategoryId(incomeCategory.getId());

        when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
        when(categoryRepository.findById(incomeCategory.getId())).thenReturn(Optional.of(incomeCategory));
        
        when(incomeRepository.save(any(Income.class))).thenAnswer(i -> {
            Income inc = i.getArgument(0);
            inc.setId(UUID.randomUUID());
            return inc;
        });

        // WHEN
        IncomeResponse res = incomeService.createIncome(req);

        // THEN
        assertNotNull(res);
        assertEquals("Salary", res.getTitle());
        assertEquals(CategoryType.INCOME.name(), incomeCategory.getType().name());
    }

    @Test
    @DisplayName("Create: Should fail if Category is EXPENSE type")
    void createIncome_Fail_WrongType() {
        // GIVEN
        Category expenseCat = Category.builder().type(CategoryType.EXPENSE).build();
        
        IncomeRequest req = new IncomeRequest();
        req.setCategoryId(UUID.randomUUID());

        when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
        when(categoryRepository.findById(any())).thenReturn(Optional.of(expenseCat));

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class, () -> incomeService.createIncome(req));
    }

    // --- UPDATE TESTS ---
    @Test
    @DisplayName("Update: Should fail if user is not owner")
    void updateIncome_Fail_NotOwner() {
        // GIVEN
        User otherUser = User.builder().build();
        otherUser.setId(UUID.randomUUID());

        Income otherIncome = new Income();
        otherIncome.setId(UUID.randomUUID());
        otherIncome.setUser(otherUser);

        when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
        when(incomeRepository.findById(any())).thenReturn(Optional.of(otherIncome));

        // WHEN & THEN
        assertThrows(OperationNotPermittedException.class, () -> 
            incomeService.updateIncome(otherIncome.getId(), new IncomeRequest())
        );
    }

    // --- DELETE TESTS ---
    @Test
    @DisplayName("Delete: Should succeed if owner")
    void deleteIncome_Success() {
        UUID id = UUID.randomUUID();
        Income income = new Income();
        income.setId(id);
        income.setUser(mockUser);

        when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
        when(incomeRepository.findById(id)).thenReturn(Optional.of(income));

        incomeService.deleteIncome(id);

        verify(incomeRepository).delete(income);
    }

    // --- GET DETAIL TEST ---
    @Test
    @DisplayName("Get Detail: Should return data if owner")
    void getIncomeById_Success() {
        UUID id = UUID.randomUUID();
        Income income = new Income();
        income.setId(id);
        income.setTitle("Bonus");
        income.setAmount(BigDecimal.TEN);
        income.setUser(mockUser);
        income.setCategory(incomeCategory);

        when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
        when(incomeRepository.findById(id)).thenReturn(Optional.of(income));

        IncomeResponse res = incomeService.getIncomeById(id);

        assertEquals("Bonus", res.getTitle());
    }
}
