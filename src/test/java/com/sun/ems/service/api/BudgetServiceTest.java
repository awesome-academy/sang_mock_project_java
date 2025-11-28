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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

	@Mock
	private BudgetRepository budgetRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private CategoryRepository categoryRepository;

	@InjectMocks
	private BudgetService budgetService;

	private User mockUser;
	private Category mockCategory;
	private final String TEST_EMAIL = "user@test.com";

	@BeforeEach
	void setUp() {
		// 1. Mock Security Context
		Authentication authentication = mock(Authentication.class);
		SecurityContext securityContext = mock(SecurityContext.class);
		when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);

		// 2. Mock User
		mockUser = User.builder().email(TEST_EMAIL).name("Test User").build();
		mockUser.setId(UUID.randomUUID());

		// 3. Mock Category
		mockCategory = Category.builder().name("Food").user(mockUser).build();
		mockCategory.setId(UUID.randomUUID());
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	// --- TEST GET BUDGETS ---
	@Test
	@DisplayName("Get All: Should return list by User ID when period is null")
	void getBudgets_NoPeriod() {
		when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn(TEST_EMAIL);
		when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
		when(budgetRepository.findByUserId(mockUser.getId())).thenReturn(Collections.emptyList());

		List<BudgetResponse> result = budgetService.getBudgets(null);

		assertNotNull(result);
		verify(budgetRepository).findByUserId(mockUser.getId());
		verify(budgetRepository, never()).findByUserIdAndPeriod(any(), any());
	}

	@Test
	@DisplayName("Get All: Should return list by Period when period is provided")
	void getBudgets_WithPeriod() {
		when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn(TEST_EMAIL);
		when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
		when(budgetRepository.findByUserIdAndPeriod(mockUser.getId(), "11-2025")).thenReturn(Collections.emptyList());

		budgetService.getBudgets("11-2025");

		verify(budgetRepository).findByUserIdAndPeriod(mockUser.getId(), "11-2025");
	}

	// --- TEST CREATE ---
	@Test
	@DisplayName("Create: Should succeed for specific Category")
	void createBudget_Success_Category() {
		// GIVEN
		BudgetRequest req = new BudgetRequest();
		req.setName("Food Budget");
		req.setAmount(new BigDecimal("500000"));
		req.setPeriod("11-2025");
		req.setCategoryId(mockCategory.getId());

		when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn(TEST_EMAIL);
		when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
		when(categoryRepository.findById(mockCategory.getId())).thenReturn(Optional.of(mockCategory));
		when(budgetRepository.existsByUserIdAndCategoryIdAndPeriod(any(), any(), any())).thenReturn(false);

		when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> {
			Budget b = i.getArgument(0);
			b.setId(UUID.randomUUID());
			return b;
		});

		// WHEN
		BudgetResponse res = budgetService.createBudget(req);

		// THEN
		assertNotNull(res);
		assertEquals("Food", res.getCategoryName());
		verify(budgetRepository).save(any(Budget.class));
	}

	@Test
	@DisplayName("Create: Should fail if Duplicate Budget for same Category and Period")
	void createBudget_Fail_Duplicate() {
		// GIVEN
		BudgetRequest req = new BudgetRequest();
		req.setPeriod("11-2025");
		req.setCategoryId(mockCategory.getId());

		when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn(TEST_EMAIL);
		when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
		when(categoryRepository.findById(mockCategory.getId())).thenReturn(Optional.of(mockCategory));
		when(budgetRepository.existsByUserIdAndCategoryIdAndPeriod(any(), any(), any())).thenReturn(true);

		// WHEN & THEN
		assertThrows(IllegalArgumentException.class, () -> budgetService.createBudget(req));
		verify(budgetRepository, never()).save(any());
	}

	@Test
	@DisplayName("Create: Should succeed for Global Budget (No Category)")
	void createBudget_Success_Global() {
		// GIVEN
		BudgetRequest req = new BudgetRequest();
		req.setName("Monthly Total");
		req.setPeriod("11-2025");
		req.setCategoryId(null);

		when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn(TEST_EMAIL);
		when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
		when(budgetRepository.existsByUserIdAndCategoryIsNullAndPeriod(any(), any())).thenReturn(false);

		when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> {
			Budget b = i.getArgument(0);
			b.setId(UUID.randomUUID());
			return b;
		});

		// WHEN
		BudgetResponse res = budgetService.createBudget(req);

		// THEN
		assertEquals("General Budget", res.getCategoryName());
		verify(budgetRepository).save(any(Budget.class));
	}

	// --- TEST UPDATE ---
	@Test
	@DisplayName("Update: Should succeed when updating simple fields (Name/Amount)")
	void updateBudget_Success_Simple() {
		// GIVEN
		UUID budgetId = UUID.randomUUID();
		Budget existingBudget = new Budget();
		existingBudget.setId(budgetId);
		existingBudget.setUser(mockUser);
		existingBudget.setPeriod("11-2025");
		existingBudget.setCategory(mockCategory);

		BudgetRequest req = new BudgetRequest();
		req.setName("New Name");
		req.setAmount(new BigDecimal("900000"));
		req.setPeriod("11-2025");
		req.setCategoryId(mockCategory.getId());

		when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn(TEST_EMAIL);
		when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
		when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(existingBudget));
		when(budgetRepository.save(any())).thenAnswer(i -> i.getArgument(0));

		// WHEN
		BudgetResponse res = budgetService.updateBudget(budgetId, req);

		// THEN
		assertEquals("New Name", res.getName());

		verify(budgetRepository, never()).existsByUserIdAndCategoryIdAndPeriodAndIdNot(any(), any(), any(), any());
	}

	@Test
	@DisplayName("Update: Should fail if changing to an existing Period/Category combo")
	void updateBudget_Fail_DuplicateChange() {
		// GIVEN
		UUID budgetId = UUID.randomUUID();
		Budget existingBudget = new Budget();
		existingBudget.setId(budgetId);
		existingBudget.setUser(mockUser);
		existingBudget.setPeriod("11-2025");
		existingBudget.setCategory(mockCategory);

		BudgetRequest req = new BudgetRequest();
		req.setPeriod("12-2025");
		req.setCategoryId(mockCategory.getId());

		when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn(TEST_EMAIL);
		when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
		when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(existingBudget));
		when(categoryRepository.findById(mockCategory.getId())).thenReturn(Optional.of(mockCategory));

		when(budgetRepository.existsByUserIdAndCategoryIdAndPeriodAndIdNot(any(), any(), any(), any()))
				.thenReturn(true);

		// WHEN & THEN
		assertThrows(IllegalArgumentException.class, () -> budgetService.updateBudget(budgetId, req));
	}

	@Test
	@DisplayName("Update: Should fail if user is not owner")
	void updateBudget_Fail_NotOwner() {
		// GIVEN
		User otherUser = User.builder().build();
		otherUser.setId(UUID.randomUUID());
		Budget otherBudget = new Budget();
		otherBudget.setId(UUID.randomUUID());
		otherBudget.setUser(otherUser);

		when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn(TEST_EMAIL);
		when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
		when(budgetRepository.findById(any())).thenReturn(Optional.of(otherBudget));

		// WHEN & THEN
		assertThrows(OperationNotPermittedException.class,
				() -> budgetService.updateBudget(otherBudget.getId(), new BudgetRequest()));
	}

	// --- TEST DELETE ---
	@Test
	@DisplayName("Delete: Should succeed if owner")
	void deleteBudget_Success() {
		UUID budgetId = UUID.randomUUID();
		Budget budget = new Budget();
		budget.setId(budgetId);
		budget.setUser(mockUser);

		when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn(TEST_EMAIL);
		when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
		when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));

		budgetService.deleteBudget(budgetId);

		verify(budgetRepository).delete(budget);
	}
}
