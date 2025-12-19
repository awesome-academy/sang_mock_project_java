package com.example.ems.service.admin;

import com.example.ems.constant.CategoryType;
import com.example.ems.dto.request.CategoryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface CategoryService {
    Page<CategoryDto> getGlobalCategories(String keyword, CategoryType type, Pageable pageable);
    CategoryDto getCategoryById(UUID id);
    void saveCategory(CategoryDto dto);
    void updateCategory(UUID id, CategoryDto dto);
    void deleteCategory(UUID id);
}
