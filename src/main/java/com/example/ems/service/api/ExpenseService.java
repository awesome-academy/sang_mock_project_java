package com.example.ems.service.api;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.ems.constant.CategoryType;
import com.example.ems.dto.request.ExpenseFilterRequest;
import com.example.ems.dto.request.ExpenseRequest;
import com.example.ems.dto.response.ExpenseDetailResponse;
import com.example.ems.dto.response.ExpenseListResponse;
import com.example.ems.dto.response.ExpenseResponse;
import com.example.ems.dto.response.PageResponse;
import com.example.ems.entity.Attachment;
import com.example.ems.entity.Budget;
import com.example.ems.entity.Category;
import com.example.ems.entity.Expense;
import com.example.ems.entity.User;
import com.example.ems.exception.OperationNotPermittedException;
import com.example.ems.exception.ResourceNotFoundException;
import com.example.ems.repository.AttachmentRepository;
import com.example.ems.repository.BudgetRepository;
import com.example.ems.repository.CategoryRepository;
import com.example.ems.repository.ExpenseRepository;
import com.example.ems.repository.ExpenseSpecification;
import com.example.ems.repository.UserRepository;
import com.example.ems.repository.projection.MonthlyStat;
import com.example.ems.service.FileStorageService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

	private static final String GLOBAL_ALERT_TEMPLATE = "Warning: Total spending for %s has exceeded the global budget by %s VND";
    private static final String CATEGORY_ALERT_TEMPLATE = "Warning: Spending for category '%s' in %s has exceeded the budget by %s VND";
    
    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
    
    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> getExpensesWithFilter(ExpenseFilterRequest filter) {
        User user = getCurrentUser();

        int pageNo = filter.getPage() < 1 ? 0 : filter.getPage() - 1;
        Pageable pageable = PageRequest.of(pageNo, filter.getSize());

        Page<Expense> expensePage = expenseRepository.findAll(
                ExpenseSpecification.getFilter(user.getId(), filter), 
                pageable
        );

        List<ExpenseResponse> content = expensePage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        List<String> alerts = calculateAlertsForList(user, expensePage.getContent());

        return PageResponse.<ExpenseResponse>builder()
                .content(content)
                .pageNo(expensePage.getNumber() + 1)
                .pageSize(expensePage.getSize())
                .totalElements(expensePage.getTotalElements())
                .totalPages(expensePage.getTotalPages())
                .last(expensePage.isLast())
                .globalAlerts(alerts)
                .build();
    }

    private List<String> calculateAlertsForList(User user, List<Expense> expenses) {
        if (expenses.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. Determine the date range (Min Date -> Max Date) of the current list
        // This helps to limit the scope of the database query.
        LocalDate minDate = expenses.stream()
                .map(Expense::getExpenseDate)
                .min(Comparator.naturalOrder())
                .orElse(LocalDate.now());

        LocalDate maxDate = expenses.stream()
                .map(Expense::getExpenseDate)
                .max(Comparator.naturalOrder())
                .orElse(LocalDate.now());

        // Extend to the first day of the min month and the last day of the max month
        // to ensure accurate total calculation for the whole month.
        LocalDate startDate = YearMonth.from(minDate).atDay(1);
        LocalDate endDate = YearMonth.from(maxDate).atEndOfMonth();

        // 2. Batch Query 1: Calculate total spending per month within the range
        // Returns a list of (Year, Month, TotalAmount)
        List<MonthlyStat> stats = expenseRepository.findMonthlyStats(user.getId(), startDate, endDate);

        // Convert List -> Map for fast lookup: Key "MM-yyyy" -> Value TotalAmount
        Map<String, BigDecimal> monthlyTotals = stats.stream()
                .collect(Collectors.toMap(
                        s -> String.format("%02d-%d", s.getMonth(), s.getYear()), // Key format: 11-2025
                        MonthlyStat::getTotalAmount
                ));

        // 3. Batch Query 2: Retrieve Global Budgets for relevant months
        // We only care about months that actually have spending data
        Set<String> periodsToCheck = monthlyTotals.keySet();
        
        // If no spending data found (unlikely if expenses list is not empty), return empty list
        if (periodsToCheck.isEmpty()) {
             return new ArrayList<>();
        }

        List<Budget> budgets = budgetRepository.findByUserIdAndCategoryIsNullAndPeriodIn(user.getId(), periodsToCheck);

        // 4. Compare and Generate Alerts (In-Memory Processing)
        List<String> alerts = new ArrayList<>();

        for (Budget budget : budgets) {
            String period = budget.getPeriod();
            BigDecimal totalSpent = monthlyTotals.getOrDefault(period, BigDecimal.ZERO);

            if (totalSpent.compareTo(budget.getAmount()) > 0) {
                BigDecimal exceeded = totalSpent.subtract(budget.getAmount());
                
                // Alert Message in English
                alerts.add(formatAlertMessage(period, null, exceeded));            }
        }

        return alerts;
    }

    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request) {
        User user = getCurrentUser();

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (category.getType() == CategoryType.INCOME) {
            throw new IllegalArgumentException("Unable to create expense for Income category");
        }

        Expense expense = new Expense();
        expense.setTitle(request.getTitle());
        expense.setAmount(request.getAmount());
        expense.setExpenseDate(request.getExpenseDateAsLocalDate());
        expense.setNote(request.getNote());
        expense.setCategory(category);
        expense.setUser(user);

        Expense savedExpense = expenseRepository.save(expense);

        String alertMessage = checkBudgetExceeded(user, category, request.getAmount(), request.getExpenseDateAsLocalDate());
        
        ExpenseResponse response = mapToResponse(savedExpense);
        response.setBudgetAlert(alertMessage);

        return response;
    }

    @Transactional
    public ExpenseResponse updateExpense(UUID id, ExpenseRequest request) {
        User user = getCurrentUser();

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        if (!expense.getUser().getId().equals(user.getId())) {
            throw new OperationNotPermittedException("You do not have permission to edit this expense");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (category.getType() == CategoryType.INCOME) {
            throw new IllegalArgumentException("Unable to assign expense to Income category");
        }

        expense.setTitle(request.getTitle());
        expense.setAmount(request.getAmount());
        expense.setExpenseDate(request.getExpenseDateAsLocalDate()); 
        expense.setNote(request.getNote());
        expense.setCategory(category);

        Expense savedExpense = expenseRepository.save(expense);

        String alertMessage = checkBudgetExceeded(
            user, 
            category, 
            request.getAmount(), 
            request.getExpenseDateAsLocalDate()
        );

        ExpenseResponse response = mapToResponse(savedExpense);
        response.setBudgetAlert(alertMessage);

        return response;
    }
    
    @Transactional
    public List<String> uploadAttachments(UUID expenseId, List<MultipartFile> files) {
        User user = getCurrentUser();
        
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        
        if (!expense.getUser().getId().equals(user.getId())) {
            throw new OperationNotPermittedException("You do not have permission to add attachments to this expense");
        }

        List<String> uploadedUrls = new ArrayList<>();
        List<Attachment> attachmentsToSave = new ArrayList<>();

        for (MultipartFile file : files) {
            String savedFileName = fileStorageService.storeFile(file);
            String fileUrl = "/uploads/" + savedFileName;

            Attachment attachment = Attachment.builder()
                    .fileName(savedFileName)
                    .fileType(file.getContentType())
                    .filePath(fileUrl)
                    .expense(expense)
                    .build();
            
            attachmentsToSave.add(attachment);
            uploadedUrls.add(fileUrl);
        }
        
        attachmentRepository.saveAll(attachmentsToSave);
        
        return uploadedUrls;
    }

    // 2. Get Expense Detail Logic
    @Transactional(readOnly = true)
    public ExpenseDetailResponse getExpenseById(UUID id) {
        User user = getCurrentUser();
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        if (!expense.getUser().getId().equals(user.getId())) {
            // Updated message
            throw new OperationNotPermittedException("You do not have permission to view this expense");
        }

        List<String> attachmentUrls = expense.getAttachments().stream()
                .map(Attachment::getFilePath)
                .collect(Collectors.toList());

        return ExpenseDetailResponse.builder()
                .id(expense.getId())
                .title(expense.getTitle())
                .amount(expense.getAmount())
                .expenseDate(expense.getExpenseDate())
                .note(expense.getNote())
                .categoryId(expense.getCategory().getId())
                .categoryName(expense.getCategory().getName())
                .categoryIcon(expense.getCategory().getIcon())
                .categoryType(expense.getCategory().getType().name())
                .attachments(attachmentUrls)
                .build();
    }
    
    // 3. Delete Expense
    @Transactional
    public void deleteExpense(UUID id) {
        User user = getCurrentUser();
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        if (!expense.getUser().getId().equals(user.getId())) {
            throw new OperationNotPermittedException("You do not have permission to delete this expense");
        }
        expenseRepository.delete(expense);
    }

    private ExpenseResponse mapToResponse(Expense expense) {
    	List<String> attachmentUrls = Optional.ofNullable(expense.getAttachments())
                .orElse(Collections.emptyList())
                .stream()
                .map(Attachment::getFilePath)
                .collect(Collectors.toList());
        
        return ExpenseResponse.builder()
                .id(expense.getId())
                .title(expense.getTitle())
                .amount(expense.getAmount())
                .expenseDate(expense.getExpenseDate())
                .note(expense.getNote())
                .categoryId(expense.getCategory().getId())
                .categoryName(expense.getCategory().getName())
                .categoryIcon(expense.getCategory().getIcon())
                .attachments(attachmentUrls)
                .build();
    }

    private String checkBudgetExceeded(User user, Category category, BigDecimal newAmount, LocalDate date) {
        String period = date.format(DateTimeFormatter.ofPattern("MM-yyyy"));
        
        YearMonth yearMonth = YearMonth.from(date);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Budget> catBudgets = budgetRepository.findByUserIdAndPeriod(user.getId(), period);
        
        Optional<Budget> targetBudget = catBudgets.stream()
                .filter(b -> b.getCategory() != null && b.getCategory().getId().equals(category.getId()))
                .findFirst();

        if (targetBudget.isPresent()) {
            return calculateAlert(user.getId(), category.getId(), startDate, endDate, newAmount, targetBudget.get(), period);
        }

        Optional<Budget> globalBudget = catBudgets.stream()
                .filter(b -> b.getCategory() == null)
                .findFirst();

        if (globalBudget.isPresent()) {
            return calculateGlobalAlert(user.getId(), startDate, endDate, newAmount, globalBudget.get(), period);
        }

        return null;
    }

    private String calculateAlert(UUID userId, UUID catId, LocalDate start, LocalDate end, 
            BigDecimal newAmount, Budget budget, String period
    ) {
		BigDecimal totalUsed = expenseRepository.sumAmountByCategoryAndDateRange(userId, catId, start, end);
		if (totalUsed == null) totalUsed = BigDecimal.ZERO;
		
		if (totalUsed.compareTo(budget.getAmount()) > 0) {
			BigDecimal exceeded = totalUsed.subtract(budget.getAmount());
			
			return formatAlertMessage(period, budget.getCategory().getName(), exceeded);
		}
		
		return null;
	}

    private String calculateGlobalAlert(UUID userId, LocalDate start, LocalDate end, 
            BigDecimal newAmount, Budget budget, String period
    ) {
		BigDecimal totalUsed = expenseRepository.sumTotalAmountByDateRange(userId, start, end);
		if (totalUsed == null) totalUsed = BigDecimal.ZERO;
		
		if (totalUsed.compareTo(budget.getAmount()) > 0) {
			BigDecimal exceeded = totalUsed.subtract(budget.getAmount());
			
			return formatAlertMessage(period, null, exceeded);
		}
		return null;
	}
    
    private String formatAlertMessage(String period, String categoryName, BigDecimal exceededAmount) {
        String formattedAmount = String.format("%,.2f", exceededAmount);

        if (categoryName == null) {
            return String.format(GLOBAL_ALERT_TEMPLATE, period, formattedAmount);
        } else {
            return String.format(CATEGORY_ALERT_TEMPLATE, categoryName, period, formattedAmount);
        }
    }
}
