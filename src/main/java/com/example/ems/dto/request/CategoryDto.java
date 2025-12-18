package com.example.ems.dto.request;

import com.example.ems.constant.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class CategoryDto {

    private UUID id;

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private String icon;

    @NotNull(message = "Type is required")
    private CategoryType type;
}
