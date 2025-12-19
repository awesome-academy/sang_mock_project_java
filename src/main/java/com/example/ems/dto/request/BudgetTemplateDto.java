package com.example.ems.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
public class BudgetTemplateDto {

    private UUID id;

    @NotBlank(message = "Template name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "Period must be in YYYY-MM format (e.g., 2024-05)")
    private String period;

    @NotNull(message = "Default amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    private BigDecimal defaultAmount;

    private String defaultCategories;
}
