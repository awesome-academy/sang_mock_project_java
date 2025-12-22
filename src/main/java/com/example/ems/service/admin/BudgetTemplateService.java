package com.example.ems.service.admin;

import com.example.ems.dto.request.BudgetTemplateDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface BudgetTemplateService {
    Page<BudgetTemplateDto> getTemplates(String keyword, Pageable pageable);
    BudgetTemplateDto getTemplateById(UUID id);
    void saveTemplate(BudgetTemplateDto dto);
    void updateTemplate(UUID id, BudgetTemplateDto dto);
    void deleteTemplate(UUID id);
}
