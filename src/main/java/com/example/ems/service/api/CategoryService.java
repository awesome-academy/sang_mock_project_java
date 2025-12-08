package com.example.ems.service.api;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ems.constant.CategoryType;
import com.example.ems.dto.request.CategoryRequest;
import com.example.ems.dto.response.CategoryResponse;
import com.example.ems.entity.Category;
import com.example.ems.entity.User;
import com.example.ems.exception.OperationNotPermittedException;
import com.example.ems.exception.ResourceNotFoundException;
import com.example.ems.repository.CategoryRepository;
import com.example.ems.repository.UserRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public List<CategoryResponse> getAllCategories() {
        User currentUser = getCurrentUser();
        List<Category> categories = categoryRepository.findAllByUserIdOrGlobal(currentUser.getId());

        return categories.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        User currentUser = getCurrentUser();

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .icon(request.getIcon())
                .type(parseCategoryType(request.getType()))
                .user(currentUser)
                .isDeleted(false)
                .build();

        Category savedCategory = categoryRepository.save(category);
        return mapToResponse(savedCategory);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        User currentUser = getCurrentUser();
        Category category = getCategoryIfOwnedByUser(id, currentUser);

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setIcon(request.getIcon());
        category.setType(parseCategoryType(request.getType()));
        Category updatedCategory = categoryRepository.save(category);
        
        return mapToResponse(updatedCategory);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        User currentUser = getCurrentUser();
        Category category = getCategoryIfOwnedByUser(id, currentUser);

        category.setIsDeleted(true);
        categoryRepository.save(category);
    }

    private Category getCategoryIfOwnedByUser(UUID id, User user) {
        Category category = categoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (category.getUser() == null) {
            throw new OperationNotPermittedException("You cannot modify or delete global categories.");
        }

        if (!category.getUser().getId().equals(user.getId())) {
            throw new OperationNotPermittedException("You do not have permission to access or modify this category.");
        }

        return category;
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .icon(category.getIcon())
                .type(category.getType())
                .isGlobal(category.getUser() == null)
                .build();
    }
    
    private CategoryType parseCategoryType(String type) {
        try {
            return CategoryType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid type");
        }
    }
}
