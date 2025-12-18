package com.example.ems.controller.admin;

import com.example.ems.constant.CategoryType;
import com.example.ems.dto.request.CategoryDto;
import com.example.ems.service.admin.CategoryService;
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
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public String listCategories(Model model,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "") String keyword,
                                 @RequestParam(required = false) CategoryType type) {
        
        Pageable pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending());
        Page<CategoryDto> pageData = categoryService.getGlobalCategories(keyword, type, pageable);

        model.addAttribute("categories", pageData);
        model.addAttribute("currentPage", page);
        model.addAttribute("keyword", keyword);
        model.addAttribute("type", type);
        
        return "admin/categories/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("categoryDto", new CategoryDto());
        model.addAttribute("pageTitle", "Create Category");
        return "admin/categories/form";
    }

    @PostMapping("/save")
    public String saveCategory(@Valid @ModelAttribute("categoryDto") CategoryDto dto,
                               BindingResult result,
                               Model model,
                               RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Create Category");
            return "admin/categories/form";
        }
        try {
            categoryService.saveCategory(dto);
            ra.addFlashAttribute("message", "Category created successfully!");
            return "redirect:/admin/categories";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("pageTitle", "Create Category");
            return "admin/categories/form";
        } catch (Exception e) {
            model.addAttribute("error", "An unexpected error occurred. Please try again.");
            model.addAttribute("pageTitle", "Create Category");
            return "admin/categories/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable UUID id, Model model, RedirectAttributes ra) {
        try {
            CategoryDto dto = categoryService.getCategoryById(id);
            model.addAttribute("categoryDto", dto);
            model.addAttribute("pageTitle", "Edit Category");
            return "admin/categories/form";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Category not found");
            return "redirect:/admin/categories";
        }
    }

    @PostMapping("/update/{id}")
    public String updateCategory(@PathVariable UUID id,
                                 @Valid @ModelAttribute("categoryDto") CategoryDto dto,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Edit Category");
            return "admin/categories/form";
        }
        try {
            categoryService.updateCategory(id, dto);
            ra.addFlashAttribute("message", "Category updated successfully!");
            return "redirect:/admin/categories";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("pageTitle", "Edit Category");
            return "admin/categories/form";
        } catch (Exception e) {
            model.addAttribute("error", "An unexpected error occurred.");
            model.addAttribute("pageTitle", "Edit Category");
            return "admin/categories/form";
        }
    }
    
    @PostMapping("/delete/{id}") 
    public String deleteCategory(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            categoryService.deleteCategory(id);
            ra.addFlashAttribute("message", "Category deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error deleting category.");
        }
        return "redirect:/admin/categories";
    }
}
