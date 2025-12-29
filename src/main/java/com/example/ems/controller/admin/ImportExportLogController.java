package com.example.ems.controller.admin;

import com.example.ems.dto.request.ImportLogFilterRequest;
import com.example.ems.entity.ImportExportLog;
import com.example.ems.service.csv.ImportExportLogService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/import-logs")
@RequiredArgsConstructor
public class ImportExportLogController {
	private final ImportExportLogService logService;

    @GetMapping
    public String listLogs(Model model,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           @ModelAttribute("filter") ImportLogFilterRequest filter) {

        Page<ImportExportLog> logs = logService.getLogs(
                filter, 
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        model.addAttribute("logs", logs);
        model.addAttribute("currentPage", page);
        
        return "admin/import-logs/list";
    }
}
