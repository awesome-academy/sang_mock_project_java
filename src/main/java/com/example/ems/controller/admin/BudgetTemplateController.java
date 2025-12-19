package com.example.ems.controller.admin;

import com.example.ems.dto.request.BudgetTemplateDto;
import com.example.ems.service.admin.BudgetTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/admin/templates")
@RequiredArgsConstructor
public class BudgetTemplateController {

    private final BudgetTemplateService templateService;

    @GetMapping
    public String listTemplates(Model model,
								@RequestParam(defaultValue = "0") int page,
								@RequestParam(defaultValue = "") String keyword) {
        
        Pageable pageable = PageRequest.of(page, 10, Sort.by("name").ascending());

        Page<BudgetTemplateDto> pageData = templateService.getTemplates(keyword, pageable);

        model.addAttribute("templates", pageData);
        model.addAttribute("currentPage", page);
        model.addAttribute("keyword", keyword);
        
        return "admin/templates/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("templateDto", new BudgetTemplateDto());
        model.addAttribute("pageTitle", "Create Budget Template");
        return "admin/templates/form";
    }

    @PostMapping("/save")
    public String saveTemplate(@Valid @ModelAttribute("templateDto") BudgetTemplateDto dto,
                               BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Create Budget Template");
            
            return "admin/templates/form";
        }
        try {
            templateService.saveTemplate(dto);
            ra.addFlashAttribute("message", "Template created successfully!");
            
            return "redirect:/admin/templates";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("pageTitle", "Create Budget Template");
            
            return "admin/templates/form";
        } catch (Exception e) {
            model.addAttribute("error", "An unexpected error occurred.");
            model.addAttribute("pageTitle", "Create Budget Template");

            return "admin/templates/form";
        }
    }
    
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable UUID id, Model model, RedirectAttributes ra) {
        try {
            BudgetTemplateDto dto = templateService.getTemplateById(id);
            model.addAttribute("templateDto", dto);
            model.addAttribute("pageTitle", "Edit Template");
            
            return "admin/templates/form";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Template not found");
            
            return "redirect:/admin/templates";
        }
    }

    @PostMapping("/update/{id}")
    public String updateTemplate(@PathVariable UUID id,
                                 @Valid @ModelAttribute("templateDto") BudgetTemplateDto dto,
                                 BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Edit Template");
            
            return "admin/templates/form";
        }
        try {
            templateService.updateTemplate(id, dto);
            ra.addFlashAttribute("message", "Template updated successfully!");
            
            return "redirect:/admin/templates";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("pageTitle", "Edit Template");
            
            return "admin/templates/form";
        } catch (Exception e) {
            model.addAttribute("error", "An unexpected error occurred.");
            model.addAttribute("pageTitle", "Edit Template");

            return "admin/templates/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteTemplate(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            templateService.deleteTemplate(id);
            ra.addFlashAttribute("message", "Template deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error deleting template.");
        }
        return "redirect:/admin/templates";
    }
}
