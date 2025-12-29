package com.example.ems.service.admin;

import com.example.ems.annotation.LogActivity;
import com.example.ems.constant.EntityType;
import com.example.ems.constant.LogAction;
import com.example.ems.dto.request.BudgetTemplateDto;
import com.example.ems.entity.BudgetTemplate;
import com.example.ems.exception.ResourceNotFoundException;
import com.example.ems.repository.BudgetTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BudgetTemplateServiceImpl implements BudgetTemplateService {

    private final BudgetTemplateRepository templateRepository;

    @Override
    public Page<BudgetTemplateDto> getTemplates(String keyword, Pageable pageable) {
        return templateRepository.searchTemplates(keyword, pageable)
                .map(this::mapToDto);
    }

    @Override
    public BudgetTemplateDto getTemplateById(UUID id) {
        BudgetTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        return mapToDto(template);
    }

    @Override
    @Transactional
    @LogActivity(action = LogAction.CREATE, entityType = EntityType.TEMPLATE, entityClass = BudgetTemplate.class)
    public void saveTemplate(BudgetTemplateDto dto) {
        if (templateRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("Template name already exists");
        }
        BudgetTemplate template = new BudgetTemplate();
        mapToEntity(dto, template);
        templateRepository.save(template);
    }

    @Override
    @Transactional
    @LogActivity(action = LogAction.UPDATE, entityType = EntityType.TEMPLATE, entityClass = BudgetTemplate.class)
    public void updateTemplate(UUID id, BudgetTemplateDto dto) {
        BudgetTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        
        String existingName = template.getName();
        String newName = dto.getName();
        boolean nameChanged =
                (existingName == null && newName != null) ||
                (existingName != null && !existingName.equalsIgnoreCase(newName));
        if (nameChanged && templateRepository.existsByNameIgnoreCase(newName)) {
             throw new IllegalArgumentException("Template name already exists");
        }
        
        mapToEntity(dto, template);
        templateRepository.save(template);
    }

    @Override
    @Transactional
    @LogActivity(action = LogAction.DELETE, entityType = EntityType.TEMPLATE, entityClass = BudgetTemplate.class)
    public void deleteTemplate(UUID id) {
    	BudgetTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        templateRepository.delete(template);
    }

    private void mapToEntity(BudgetTemplateDto dto, BudgetTemplate entity) {
        entity.setName(dto.getName());
        entity.setPeriod(dto.getPeriod());
        entity.setDefaultAmount(dto.getDefaultAmount());
        entity.setDefaultCategories(dto.getDefaultCategories());
    }

    private BudgetTemplateDto mapToDto(BudgetTemplate entity) {
        BudgetTemplateDto dto = new BudgetTemplateDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setPeriod(entity.getPeriod());
        dto.setDefaultAmount(entity.getDefaultAmount());
        dto.setDefaultCategories(entity.getDefaultCategories());
        return dto;
    }
}
