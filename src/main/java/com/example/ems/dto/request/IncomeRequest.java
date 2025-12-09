package com.example.ems.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import com.example.ems.exception.InvalidDateException;
import com.example.ems.validation.ValidDate;

@Data
public class IncomeRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Income date is required")
    @ValidDate()
    private String incomeDate;
    
    private String note;

    @NotNull(message = "Category is required")
    private UUID categoryId;
    
    public LocalDate getIncomeDateAsLocalDate() {
        try {
            return LocalDate.parse(this.incomeDate);
        } catch (DateTimeParseException e) {
            throw new InvalidDateException("Invalid Date");
        }
    }
}
