package com.example.ems.controller.admin;

import com.example.ems.dto.request.AdminIncomeDto;
import com.example.ems.dto.request.IncomeFilterRequest;
import com.example.ems.service.admin.IncomeService;
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
@RequestMapping("/admin/incomes")
@RequiredArgsConstructor
@Slf4j
public class IncomeController {

    private final IncomeService incomeService;

    @GetMapping
    public String listIncomes(Model model,
            @ModelAttribute IncomeFilterRequest filterRequest,
            @RequestParam(required = false) UUID userId
    ) {
        int page = filterRequest.getPage() > 0 ? filterRequest.getPage() - 1 : 0;
        Pageable pageable = PageRequest.of(page, filterRequest.getSize(), Sort.by("incomeDate").descending());
        
        Page<AdminIncomeDto> pageData = incomeService.getIncomes(filterRequest, pageable);
        
        model.addAttribute("incomes", pageData);
        model.addAttribute("currentPage", page);
        model.addAttribute("filterRequest", filterRequest);
        
        prepareFormModel(model);
        
        return "admin/incomes/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("incomeDto", new AdminIncomeDto());
        prepareFormModel(model);
        model.addAttribute("pageTitle", "Create Income");
        
        return "admin/incomes/form";
    }

    @PostMapping("/save")
    public String saveIncome(@Valid @ModelAttribute("incomeDto") AdminIncomeDto dto,
                             BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            prepareFormModel(model);
            model.addAttribute("pageTitle", "Create Income");
            
            return "admin/incomes/form";
        }
        try {
            incomeService.saveIncome(dto);
            ra.addFlashAttribute("message", "Income created successfully!");
            
            return "redirect:/admin/incomes";
        } catch (Exception e) {
            log.error("Error saving income: ", e);
            model.addAttribute("error", "An error occurred while saving the income. Please try again.");
            prepareFormModel(model);
            
            return "admin/incomes/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable UUID id, Model model, RedirectAttributes ra) {
        try {
            AdminIncomeDto dto = incomeService.getIncomeById(id);
            model.addAttribute("incomeDto", dto);
            prepareFormModel(model);
            model.addAttribute("pageTitle", "Edit Income");
            
            return "admin/incomes/form";
        } catch (Exception e) {
            log.warn("Income not found with id: {}", id);
            ra.addFlashAttribute("error", "Income record not found.");
            
            return "redirect:/admin/incomes";
        }
    }

    @PostMapping("/update/{id}")
    public String updateIncome(@PathVariable UUID id,
                               @Valid @ModelAttribute("incomeDto") AdminIncomeDto dto,
                               BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            prepareFormModel(model);
            model.addAttribute("pageTitle", "Edit Income");
            
            return "admin/incomes/form";
        }
        
        try {
            incomeService.updateIncome(id, dto);
            ra.addFlashAttribute("message", "Income updated successfully!");
            
            return "redirect:/admin/incomes";
        } catch (Exception e) {
            log.error("Error updating income ID {}: ", id, e);
            model.addAttribute("error", "An error occurred while updating. Please try again.");
            prepareFormModel(model);
            
            return "admin/incomes/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteIncome(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            incomeService.deleteIncome(id);
            ra.addFlashAttribute("message", "Income deleted successfully.");
        } catch (Exception e) {
            log.error("Failed to delete income with ID: {}", id, e); 
            ra.addFlashAttribute("error", "Could not delete the record. It might be linked to other data.");
        }
        return "redirect:/admin/incomes";
    }

    private void prepareFormModel(Model model) {
    	model.addAttribute("users", incomeService.getAllUsers());
        model.addAttribute("categories", incomeService.getIncomeCategories());
    }
}
