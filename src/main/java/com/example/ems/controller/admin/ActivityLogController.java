package com.example.ems.controller.admin;

import com.example.ems.constant.EntityType;
import com.example.ems.constant.LogAction;
import com.example.ems.dto.request.LogFilterRequest;
import com.example.ems.dto.response.ActivityLogDto;
import com.example.ems.service.ActivityLogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
@Slf4j
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @GetMapping
    public String listLogs(Model model, @ModelAttribute LogFilterRequest filterRequest) {
        int page = filterRequest.getPage() > 0 ? filterRequest.getPage() - 1 : 0;
        int size = filterRequest.getSize() > 0 ? filterRequest.getSize() : 20;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ActivityLogDto> pageData = activityLogService.getLogs(filterRequest, pageable);

        model.addAttribute("logs", pageData);
        model.addAttribute("currentPage", page);
        model.addAttribute("filterRequest", filterRequest);
        
        model.addAttribute("users", activityLogService.getAllUsers());
        model.addAttribute("actions", LogAction.values());
        model.addAttribute("entityTypes", EntityType.values());

        return "admin/logs/list";
    }
    
    @GetMapping("/{id}")
    public String viewLogDetail(@PathVariable UUID id, Model model, RedirectAttributes ra) {
        try {
            ActivityLogDto logDto = activityLogService.getLogById(id);
            model.addAttribute("log", logDto);
            
            return "admin/logs/detail";
        } catch (Exception e) {
        	log.error("Error viewing log detail", e);
            ra.addFlashAttribute("error", "Log not found or error occurred.");
            
            return "redirect:/admin/logs";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteLog(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            activityLogService.deleteLog(id);
            ra.addFlashAttribute("message", "Log deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error deleting log: " + e.getMessage());
        }
        return "redirect:/admin/logs";
    }
}
