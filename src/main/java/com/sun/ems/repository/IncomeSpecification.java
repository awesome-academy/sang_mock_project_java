package com.sun.ems.repository;

import com.sun.ems.dto.request.IncomeFilterRequest;
import com.sun.ems.entity.Income;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IncomeSpecification {

    public static Specification<Income> getFilter(UUID userId, IncomeFilterRequest req) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));

            if (req.getCategoryId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), req.getCategoryId()));
            }

            if (req.getStartDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("incomeDate"), req.getStartDate()));
            }
            if (req.getEndDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("incomeDate"), req.getEndDate()));
            }

            if (req.getKeyword() != null && !req.getKeyword().isEmpty()) {
                String escapedKeyword = escapeLikePattern(req.getKeyword().toLowerCase());
                String likePattern = "%" + escapedKeyword + "%";
                
                Predicate titleLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), likePattern, '\\');
                Predicate noteLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("note")), likePattern, '\\');
                
                predicates.add(criteriaBuilder.or(titleLike, noteLike));
            }

            query.orderBy(criteriaBuilder.desc(root.get("incomeDate")));

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
