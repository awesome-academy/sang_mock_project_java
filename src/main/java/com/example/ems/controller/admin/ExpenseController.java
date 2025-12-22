package com.example.ems.controller.admin;

import com.example.ems.constant.CategoryType;
import com.example.ems.dto.request.AdminExpenseDto;
import com.example.ems.dto.request.ExpenseFilterRequest;
import com.example.ems.repository.CategoryRepository;
import com.example.ems.repository.UserRepository;
import com.example.ems.service.admin.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin/expenses")
@RequiredArgsConstructor
@Slf4j
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public String listExpenses(Model model,
                               @ModelAttribute ExpenseFilterRequest filterRequest,
                               @RequestParam(required = false) UUID userId) {
        
        int page = filterRequest.getPage() > 0 ? filterRequest.getPage() - 1 : 0;
        Pageable pageable = PageRequest.of(page, 10, Sort.by("expenseDate").descending());
        
        Page<AdminExpenseDto> pageData = expenseService.getExpenses(
                filterRequest, 
                filterRequest.getUserId(), 
                pageable
            );
        model.addAttribute("expenses", pageData);
        model.addAttribute("currentPage", page);
        model.addAttribute("filterRequest", filterRequest);
        
        prepareFormModel(model);
        
        return "admin/expenses/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("expenseDto", new AdminExpenseDto());
        prepareFormModel(model);
        model.addAttribute("pageTitle", "Create Expense");
        return "admin/expenses/form";
    }

    @PostMapping("/save")
    public String saveExpense(@Valid @ModelAttribute("expenseDto") AdminExpenseDto dto,
                              BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            prepareFormModel(model);
            model.addAttribute("pageTitle", "Create Expense");
            return "admin/expenses/form";
        }
        try {
            expenseService.saveExpense(dto);
            ra.addFlashAttribute("message", "Expense created successfully!");
            return "redirect:/admin/expenses";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            prepareFormModel(model);
            return "admin/expenses/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable UUID id, Model model, RedirectAttributes ra) {
        try {
            AdminExpenseDto dto = expenseService.getExpenseById(id);
            model.addAttribute("expenseDto", dto);
            prepareFormModel(model);
            model.addAttribute("pageTitle", "Edit Expense");
            return "admin/expenses/form";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Expense not found");
            return "redirect:/admin/expenses";
        }
    }

    @PostMapping("/update/{id}")
    public String updateExpense(@PathVariable UUID id,
                                @Valid @ModelAttribute("expenseDto") AdminExpenseDto dto,
                                BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            prepareFormModel(model);
            model.addAttribute("pageTitle", "Edit Expense");
            return "admin/expenses/form";
        }
        try {
            expenseService.updateExpense(id, dto);
            ra.addFlashAttribute("message", "Expense updated successfully!");
            return "redirect:/admin/expenses";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            prepareFormModel(model);
            return "admin/expenses/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteExpense(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            expenseService.deleteExpense(id);
            ra.addFlashAttribute("message", "Expense deleted successfully.");
        } catch (Exception e) {
            log.error("Failed to delete expense with ID: {}", id, e);
            
            ra.addFlashAttribute("error", "Error deleting expense: " + e.getMessage());
        }
        return "redirect:/admin/expenses";
    }

    private void prepareFormModel(Model model) {
    	model.addAttribute("users", expenseService.getAllUsers());
        model.addAttribute("categories", expenseService.getIncomeCategories());
    }
}
