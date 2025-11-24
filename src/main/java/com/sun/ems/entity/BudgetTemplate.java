package com.sun.ems.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "budget_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetTemplate extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 7)
    private String period;

    @Column(name = "default_amount", precision = 15, scale = 2)
    private BigDecimal defaultAmount;

    @Column(name = "default_categories", columnDefinition = "TEXT")
    private String defaultCategories;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}
