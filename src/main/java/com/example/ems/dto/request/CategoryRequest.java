
package com.example.ems.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CategoryRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String description;
    
    private String icon;

    @NotBlank(message = "Type is required")
    @Pattern(regexp = "EXPENSE|INCOME", message = "Category type must be EXPENSE or INCOME")
    private String type;
}
