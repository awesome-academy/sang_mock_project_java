package com.example.ems.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

import com.example.ems.constant.CategoryType;

@Data
@Builder
public class CategoryResponse {
    private UUID id;
    private String name;
    private String description;
    private String icon;
    private CategoryType type;
    private boolean isGlobal;
}
