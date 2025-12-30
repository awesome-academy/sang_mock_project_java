package com.example.ems.service.csv;

import com.example.ems.constant.CategoryType;
import com.example.ems.constant.EntityType;
import com.example.ems.constant.LogAction;
import com.example.ems.dto.csv.CategoryCsvDto;
import com.example.ems.entity.Category;
import com.example.ems.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryCsvServiceImpl {

    private final CategoryRepository categoryRepository;
    private final CsvService csvService;
    private final ImportExportLogService logService;

    @Transactional(readOnly = true)
    public void exportGlobalCategories(HttpServletResponse response, String keyword, CategoryType type) {
        String fileName = "global_categories_" + System.currentTimeMillis() + ".csv";
        UUID logId = null;

        try {
            logId = logService.startLog(LogAction.EXPORT, EntityType.CATEGORY, fileName);

            try (Stream<Category> categoryStream = categoryRepository.streamGlobalCategories(keyword, type)) {
                Stream<CategoryCsvDto> dtoStream = categoryStream.map(this::mapToCsvDto);
                csvService.exportToCsvStream(response, dtoStream, CategoryCsvDto.class, fileName);
            }

            logService.finishLogSuccess(logId, "Exported global categories successfully.");

        } catch (Exception e) {
            log.error("Export categories failed", e);
            if (logId != null) logService.finishLogFailed(logId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private CategoryCsvDto mapToCsvDto(Category category) {
        return new CategoryCsvDto(
                category.getName(),
                category.getDescription(),
                category.getIcon(),
                category.getType().name()
        );
    }
}
