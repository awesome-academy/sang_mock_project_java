package com.sun.ems.repository;

import com.sun.ems.entity.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IncomeRepository extends JpaRepository<Income, UUID>, JpaSpecificationExecutor<Income> {
}
