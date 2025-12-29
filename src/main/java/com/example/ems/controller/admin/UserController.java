package com.example.ems.controller.admin;

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

import com.example.ems.constant.EntityType;
import com.example.ems.constant.LogAction;
import com.example.ems.dto.request.UserDto;
import com.example.ems.repository.RoleRepository;
import com.example.ems.service.admin.UserService;
import com.example.ems.service.csv.ImportExportLogService;
import com.example.ems.service.csv.UserCsvServiceImpl;
import com.example.ems.service.csv.UserImportAsyncService;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final RoleRepository roleRepository;
    private final UserCsvServiceImpl userCsvService;
    private final UserImportAsyncService asyncService;
    private final ImportExportLogService logService;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    
    @GetMapping
    public String listUsers(Model model,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "") String keyword,
                            @RequestParam(required = false) Boolean isActive) {
        
        Pageable pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending());
        
        Page<UserDto> userPage = userService.getUsers(keyword, isActive, pageable);

        model.addAttribute("users", userPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", isActive);
        
        return "admin/users/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new UserDto());
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("pageTitle", "Create New User");
        return "admin/users/form";
    }

    @PostMapping("/save")
    public String saveUser(@Valid @ModelAttribute("user") UserDto userDto,
                           BindingResult result,
                           Model model,
                           RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("roles", roleRepository.findAll());
            model.addAttribute("pageTitle", "Create New User");
            return "admin/users/form";
        }

        try {
            userService.saveUser(userDto);
            ra.addFlashAttribute("message", "User created successfully!");
            return "redirect:/admin/users";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", roleRepository.findAll());
            return "admin/users/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable UUID id, Model model, RedirectAttributes ra) {
        try {
            UserDto dto = userService.getUserById(id);
            model.addAttribute("user", dto);
            model.addAttribute("roles", roleRepository.findAll());
            model.addAttribute("pageTitle", "Edit User");
            return "admin/users/form";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/users";
        }
    }

    @PostMapping("/update/{id}")
    public String updateUser(@PathVariable UUID id,
                             @Valid @ModelAttribute("user") UserDto userDto,
                             BindingResult result,
                             Model model,
                             RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("roles", roleRepository.findAll());
            return "admin/users/form";
        }

        try {
            userService.updateUser(id, userDto);
            ra.addFlashAttribute("message", "User updated successfully!");
            return "redirect:/admin/users";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", roleRepository.findAll());
            return "admin/users/form";
        }
    }
    
    @GetMapping("/export")
    public void exportUsers(HttpServletResponse response,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) Boolean isActive) {
        userCsvService.exportUsers(response, keyword, isActive);
    }

    @PostMapping("/import")
    public String importUsers(@RequestParam("file") MultipartFile file, RedirectAttributes ra) {
        String fileName = file.getOriginalFilename();

        if (file.isEmpty() || fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
            ra.addFlashAttribute("error", "Invalid file format. Please upload a .csv file.");
            return "redirect:/admin/users";
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            ra.addFlashAttribute("error", "File is too large. Maximum allowed size is 5MB.");
            return "redirect:/admin/users";
        }

        try {
            UUID logId = logService.startLog(LogAction.IMPORT, EntityType.USER, fileName);

            File tempFile = File.createTempFile("users_import_" + logId + "_", ".csv");
            file.transferTo(tempFile);

            asyncService.processImport(logId, tempFile);

            ra.addFlashAttribute("message", "Import process started in background. Please check logs below.");
            return "redirect:/admin/import-logs";

        } catch (IOException e) {
            log.error("File upload failed", e); 
            ra.addFlashAttribute("error", "Error uploading file: " + e.getMessage());
            return "redirect:/admin/users";
        } catch (IllegalStateException e) {
             ra.addFlashAttribute("error", e.getMessage());
             return "redirect:/admin/auth/login";
        }
    }
}
