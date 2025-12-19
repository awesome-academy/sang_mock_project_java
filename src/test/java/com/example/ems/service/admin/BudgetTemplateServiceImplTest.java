package com.example.ems.service.admin;

import com.example.ems.dto.request.BudgetTemplateDto;
import com.example.ems.entity.BudgetTemplate;
import com.example.ems.exception.ResourceNotFoundException;
import com.example.ems.repository.BudgetTemplateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetTemplateServiceImplTest {

    @Mock
    private BudgetTemplateRepository templateRepository;

    @InjectMocks
    private BudgetTemplateServiceImpl templateService;

    // --- 1. Test Get Templates (Search) ---
    @Test
    @DisplayName("getTemplates: Should return a Page of DTOs")
    void getTemplates_Success() {
        // Arrange
        BudgetTemplate entity = new BudgetTemplate();
        entity.setId(UUID.randomUUID());
        entity.setName("Test Template");
        Page<BudgetTemplate> page = new PageImpl<>(Collections.singletonList(entity));
        
        when(templateRepository.searchTemplates(anyString(), any(Pageable.class))).thenReturn(page);

        // Act
        Page<BudgetTemplateDto> result = templateService.getTemplates("Test", PageRequest.of(0, 10));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Template", result.getContent().get(0).getName());
    }

    // --- 2. Test Get By ID ---
    @Test
    @DisplayName("getTemplateById: Should return DTO when found")
    void getTemplateById_Success() {
        UUID id = UUID.randomUUID();
        BudgetTemplate entity = new BudgetTemplate();
        entity.setId(id);
        entity.setName("Found Me");

        when(templateRepository.findById(id)).thenReturn(Optional.of(entity));

        BudgetTemplateDto result = templateService.getTemplateById(id);

        assertNotNull(result);
        assertEquals("Found Me", result.getName());
    }

    @Test
    @DisplayName("getTemplateById: Should throw ResourceNotFoundException when not found")
    void getTemplateById_NotFound() {
        UUID id = UUID.randomUUID();
        when(templateRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> templateService.getTemplateById(id));
    }

    // --- 3. Test Create (Save) ---
    @Test
    @DisplayName("saveTemplate: Should save successfully when name is unique")
    void saveTemplate_Success() {
        BudgetTemplateDto dto = new BudgetTemplateDto();
        dto.setName("New Template");
        dto.setDefaultAmount(BigDecimal.valueOf(100));
        dto.setPeriod("2025-01");

        when(templateRepository.existsByNameIgnoreCase(dto.getName())).thenReturn(false);

        templateService.saveTemplate(dto);

        verify(templateRepository).save(any(BudgetTemplate.class));
    }

    @Test
    @DisplayName("saveTemplate: Should throw Exception when name exists")
    void saveTemplate_DuplicateName() {
        BudgetTemplateDto dto = new BudgetTemplateDto();
        dto.setName("Existing Name");

        when(templateRepository.existsByNameIgnoreCase(dto.getName())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> templateService.saveTemplate(dto));
        verify(templateRepository, never()).save(any());
    }

    // --- 4. Test Update (Logic phức tạp nhất) ---
    @Test
    @DisplayName("updateTemplate: Should update logic when name is NOT changed (Case insensitive)")
    void updateTemplate_NameNotChanged_Success() {
        // Arrange
        UUID id = UUID.randomUUID();
        BudgetTemplate existing = new BudgetTemplate();
        existing.setId(id);
        existing.setName("My Template"); // Tên cũ

        BudgetTemplateDto dto = new BudgetTemplateDto();
        dto.setName("my template"); // Tên mới (chỉ khác hoa thường) -> Coi như không đổi
        dto.setDefaultAmount(BigDecimal.valueOf(200));

        when(templateRepository.findById(id)).thenReturn(Optional.of(existing));

        // Act
        templateService.updateTemplate(id, dto);

        // Assert
        // verify KHÔNG gọi check trùng DB vì tên không đổi về mặt ngữ nghĩa
        verify(templateRepository, never()).existsByNameIgnoreCase(anyString());
        verify(templateRepository).save(existing);
        assertEquals(BigDecimal.valueOf(200), existing.getDefaultAmount());
    }

    @Test
    @DisplayName("updateTemplate: Should update when name CHANGED and is UNIQUE")
    void updateTemplate_NameChanged_Unique_Success() {
        UUID id = UUID.randomUUID();
        BudgetTemplate existing = new BudgetTemplate();
        existing.setId(id);
        existing.setName("Old Name");

        BudgetTemplateDto dto = new BudgetTemplateDto();
        dto.setName("New Name"); // Khác hoàn toàn

        when(templateRepository.findById(id)).thenReturn(Optional.of(existing));
        when(templateRepository.existsByNameIgnoreCase("New Name")).thenReturn(false); // Chưa ai dùng

        templateService.updateTemplate(id, dto);

        verify(templateRepository).existsByNameIgnoreCase("New Name");
        verify(templateRepository).save(existing);
        assertEquals("New Name", existing.getName());
    }

    @Test
    @DisplayName("updateTemplate: Should throw exception when name CHANGED but DUPLICATE")
    void updateTemplate_NameChanged_Duplicate_Fail() {
        UUID id = UUID.randomUUID();
        BudgetTemplate existing = new BudgetTemplate();
        existing.setId(id);
        existing.setName("Old Name");

        BudgetTemplateDto dto = new BudgetTemplateDto();
        dto.setName("Taken Name");

        when(templateRepository.findById(id)).thenReturn(Optional.of(existing));
        when(templateRepository.existsByNameIgnoreCase("Taken Name")).thenReturn(true); // Đã có người dùng

        assertThrows(IllegalArgumentException.class, () -> templateService.updateTemplate(id, dto));
        verify(templateRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateTemplate: Should throw ResourceNotFoundException if ID not found")
    void updateTemplate_IdNotFound() {
        UUID id = UUID.randomUUID();
        when(templateRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
            () -> templateService.updateTemplate(id, new BudgetTemplateDto()));
    }

    // --- 5. Test Delete ---
    @Test
    @DisplayName("deleteTemplate: Should delete successfully if found")
    void deleteTemplate_Success() {
        UUID id = UUID.randomUUID();
        BudgetTemplate existing = new BudgetTemplate();
        existing.setId(id);

        when(templateRepository.findById(id)).thenReturn(Optional.of(existing));

        templateService.deleteTemplate(id);

        verify(templateRepository).delete(existing);
    }

    @Test
    @DisplayName("deleteTemplate: Should throw exception if not found")
    void deleteTemplate_NotFound() {
        UUID id = UUID.randomUUID();
        when(templateRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> templateService.deleteTemplate(id));
        verify(templateRepository, never()).delete(any());
    }
}
