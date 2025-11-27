package com.sun.ems.repository;

import com.sun.ems.dto.request.ExpenseFilterRequest;
import com.sun.ems.entity.Expense;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ExpenseSpecification {
    public static Specification<Expense> getFilter(UUID userId, ExpenseFilterRequest req) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (req.getKeyword() != null && !req.getKeyword().isEmpty()) {
                String escapedKeyword = escapeLikePattern(req.getKeyword().toLowerCase());
                String likePattern = "%" + escapedKeyword + "%";
                
                Predicate titleLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), likePattern);
                Predicate noteLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("note")), likePattern);
                
                predicates.add(criteriaBuilder.or(titleLike, noteLike));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String escapeLikePattern(String keyword) {
        if (keyword == null) return "";
        return keyword.replace("\\", "\\\\")
                      .replace("%", "\\%")
                      .replace("_", "\\_");
    }
}