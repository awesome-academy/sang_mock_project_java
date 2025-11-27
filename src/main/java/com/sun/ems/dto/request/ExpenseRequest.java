package com.sun.ems.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import com.sun.ems.exception.InvalidDateException;
import com.sun.ems.validation.ValidDate;

@Data
public class ExpenseRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Expense date is required")
    @ValidDate()
    private String expenseDate;

    private String note;

    @NotNull(message = "Category is required")
    private UUID categoryId;
    
    public LocalDate getExpenseDateAsLocalDate() {
        try {
            return LocalDate.parse(this.expenseDate);
        } catch (DateTimeParseException e) {
            throw new InvalidDateException("Invalid Date");
        }
    }
}
