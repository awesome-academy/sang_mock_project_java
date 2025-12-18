package com.example.ems.controller.admin;

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

import com.example.ems.dto.request.UserDto;
import com.example.ems.repository.RoleRepository;
import com.example.ems.service.admin.UserService;

import java.util.UUID;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RoleRepository roleRepository;

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
}
