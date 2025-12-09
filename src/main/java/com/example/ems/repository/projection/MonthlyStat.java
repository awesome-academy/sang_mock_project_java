package com.example.ems.repository.projection;

import java.math.BigDecimal;

public interface MonthlyStat {
    Integer getYear();
    Integer getMonth();
    BigDecimal getTotalAmount();
}
