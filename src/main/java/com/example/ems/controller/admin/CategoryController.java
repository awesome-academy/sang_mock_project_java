package com.example.ems.controller.admin;

import com.example.ems.constant.CategoryType;
import com.example.ems.constant.EntityType;
import com.example.ems.constant.LogAction;
import com.example.ems.dto.request.CategoryDto;
import com.example.ems.service.admin.CategoryService;
import com.example.ems.service.csv.CategoryImportAsyncService;
import com.example.ems.service.csv.ImportExportLogService;
import com.example.ems.service.csv.CategoryCsvServiceImpl;

import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryCsvServiceImpl categoryCsvService;
    private final CategoryImportAsyncService categoryImportService;
    private final ImportExportLogService logService;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

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
    
    @GetMapping("/export")
    public void exportCategories(HttpServletResponse response,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) CategoryType type) {
        categoryCsvService.exportGlobalCategories(response, keyword, type);
    }
    
    @PostMapping("/import")
    public String importCategories(@RequestParam("file") MultipartFile file, RedirectAttributes ra) {
        String fileName = file.getOriginalFilename();

        if (file.isEmpty() || fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
            ra.addFlashAttribute("error", "Invalid file format. Please upload a .csv file.");
            return "redirect:/admin/categories";
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            ra.addFlashAttribute("error", "File is too large. Maximum allowed size is 5MB.");
            return "redirect:/admin/categories";
        }

        try {
            UUID logId = logService.startLog(LogAction.IMPORT, EntityType.CATEGORY, fileName);

            File tempFile = File.createTempFile("category_import_" + logId + "_", ".csv");
            file.transferTo(tempFile);

            categoryImportService.processImport(logId, tempFile);

            ra.addFlashAttribute("message", "Import process started in background. Please check logs below.");
            return "redirect:/admin/import-logs";

        } catch (IOException e) {
            log.error("File upload failed", e); 
            ra.addFlashAttribute("error", "Error uploading file: " + e.getMessage());
            return "redirect:/admin/categories";
        } catch (IllegalStateException e) {
             ra.addFlashAttribute("error", e.getMessage());
             return "redirect:/admin/auth/login";
        }
    }
}
