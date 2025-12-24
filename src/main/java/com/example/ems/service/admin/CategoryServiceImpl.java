package com.example.ems.service.admin;

import com.example.ems.annotation.LogActivity;
import com.example.ems.constant.CategoryType;
import com.example.ems.constant.EntityType;
import com.example.ems.constant.LogAction;
import com.example.ems.dto.request.CategoryDto;
import com.example.ems.entity.Category;
import com.example.ems.exception.ResourceNotFoundException;
import com.example.ems.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public Page<CategoryDto> getGlobalCategories(String keyword, CategoryType type, Pageable pageable) {
        return categoryRepository.searchGlobalCategories(keyword, type, pageable)
                .map(this::mapToDto);
    }

    @Override
    public CategoryDto getCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        return mapToDto(category);
    }

    @Override
    @Transactional
    @LogActivity(action = LogAction.CREATE, entityType = EntityType.CATEGORY, entityClass = Category.class)
    public void saveCategory(CategoryDto dto) {
        if (categoryRepository.existsByNameGlobal(dto.getName(), null)) {
            throw new IllegalArgumentException("Category name already exists");
        }
        
        Category category = new Category();
        category.setUser(null);
        category.setIsDeleted(false);
        mapToEntity(dto, category);
        categoryRepository.save(category);
    }

    @Override
    @Transactional
    @LogActivity(action = LogAction.UPDATE, entityType = EntityType.CATEGORY, entityClass = Category.class)
    public void updateCategory(UUID id, CategoryDto dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (categoryRepository.existsByNameGlobal(dto.getName(), id)) {
            throw new IllegalArgumentException("Category name already exists");
        }

        mapToEntity(dto, category);
        categoryRepository.save(category);
    }

    @Override
    @Transactional
    @LogActivity(action = LogAction.DELETE, entityType = EntityType.CATEGORY, entityClass = Category.class)
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setIsDeleted(true); // Soft Delete
        categoryRepository.save(category);
    }

    private void mapToEntity(CategoryDto dto, Category entity) {
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setIcon(dto.getIcon());
        entity.setType(dto.getType());
    }

    private CategoryDto mapToDto(Category entity) {
        CategoryDto dto = new CategoryDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setIcon(entity.getIcon());
        dto.setType(entity.getType());
        return dto;
    }
}
