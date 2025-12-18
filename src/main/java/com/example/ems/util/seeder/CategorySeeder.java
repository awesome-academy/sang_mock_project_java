package com.example.ems.util.seeder;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.ems.constant.CategoryType;
import com.example.ems.entity.Category;
import com.example.ems.repository.CategoryRepository;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CategorySeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            seedExpenseCategories();
            seedIncomeCategories();
            System.out.println(">>> [Seeder] Global Categories created.");
        }
    }

    private void seedExpenseCategories() {
        List<Category> expenses = Arrays.asList(
            buildGlobalCategory("Food & Dining", "Chi phí ăn uống hàng ngày", "food_icon", CategoryType.EXPENSE),
            buildGlobalCategory("Transportation", "Đi lại, xăng xe, vé tàu xe", "bus_icon", CategoryType.EXPENSE),
            buildGlobalCategory("Utilities", "Điện, nước, internet", "bill_icon", CategoryType.EXPENSE),
            buildGlobalCategory("Shopping", "Mua sắm quần áo, đồ dùng", "bag_icon", CategoryType.EXPENSE),
            buildGlobalCategory("Health", "Thuốc men, khám bệnh", "medical_icon", CategoryType.EXPENSE),
            buildGlobalCategory("Entertainment", "Xem phim, du lịch", "movie_icon", CategoryType.EXPENSE)
        );
        categoryRepository.saveAll(expenses);
    }

    private void seedIncomeCategories() {
        List<Category> incomes = Arrays.asList(
            buildGlobalCategory("Salary", "Lương cứng hàng tháng", "money_icon", CategoryType.INCOME),
            buildGlobalCategory("Bonus", "Thưởng, hoa hồng", "gift_icon", CategoryType.INCOME),
            buildGlobalCategory("Investment", "Lãi đầu tư, tiết kiệm", "chart_icon", CategoryType.INCOME)
        );
        categoryRepository.saveAll(incomes);
    }

    private Category buildGlobalCategory(String name, String desc, String icon, CategoryType type) {
        return Category.builder()
                .name(name)
                .description(desc)
                .icon(icon)
                .type(type)
                .user(null) // Global Category
                .isDeleted(false)
                .build();
    }
}
