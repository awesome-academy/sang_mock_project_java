package com.sun.ems.dto.response;

import com.sun.ems.constant.CategoryType;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

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
