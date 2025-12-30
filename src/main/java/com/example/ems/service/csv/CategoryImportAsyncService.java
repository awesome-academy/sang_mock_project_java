package com.example.ems.service.csv;

import com.example.ems.constant.CategoryType;
import com.example.ems.constant.JobStatus;
import com.example.ems.dto.csv.CategoryCsvDto;
import com.example.ems.entity.Category;
import com.example.ems.repository.CategoryRepository;
import com.example.ems.repository.ImportExportLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryImportAsyncService {

    private final CategoryRepository categoryRepository;
    private final ImportExportLogRepository logRepository;
    private final CsvService csvService; 

    private static final String[] REQUIRED_HEADERS = {"Name", "Description", "Icon", "Type"};

    @Async
    @Transactional
    public void processImport(UUID logId, File tempFile) {
        StringBuilder errorReport = new StringBuilder();
        int successCount = 0;
        int failCount = 0;

        try {
            updateLogStatus(logId, JobStatus.IN_PROGRESS, "Validating file format...");

            // --- STEP 1: VALIDATE HEADERS ---
            try {
                csvService.validateHeaders(tempFile, REQUIRED_HEADERS);
            } catch (IllegalArgumentException e) {
                updateLogStatus(logId, JobStatus.FAILED, "CSV Header Error: " + e.getMessage());
                return;
            }

            updateLogStatus(logId, JobStatus.IN_PROGRESS, "Parsing data...");

            // --- STEP 2: PARSE CSV ---
            List<CategoryCsvDto> dtos = csvService.readCsvFromFile(tempFile, CategoryCsvDto.class);

            // --- STEP 3: PREPARE DATA ---
            Set<String> namesInFile = dtos.stream()
                    .map(CategoryCsvDto::getName)
                    .filter(name -> name != null && !name.isBlank())
                    .collect(Collectors.toSet());

            Set<String> existingNamesInDb = Collections.emptySet();
            if (!namesInFile.isEmpty()) {
                existingNamesInDb = categoryRepository.findExistingGlobalNames(namesInFile);
            }

            Set<String> processedNamesInCurrentFile = new HashSet<>();
            List<Category> buffer = new ArrayList<>();
            int BATCH_SIZE = 500;

            // --- STEP 4: PROCESS ROWS ---
            for (int i = 0; i < dtos.size(); i++) {
                CategoryCsvDto dto = dtos.get(i);
                int rowNum = i + 2;

                try {
                    // Name: Required, 2-50 chars
                    if (dto.getName() == null || dto.getName().isBlank()) {
                        throw new IllegalArgumentException("Category name is required");
                    }
                    if (dto.getName().length() < 2 || dto.getName().length() > 50) {
                        throw new IllegalArgumentException("Name must be between 2 and 50 characters");
                    }

                    // Description: Max 500 chars
                    if (dto.getDescription() != null && dto.getDescription().length() > 500) {
                        throw new IllegalArgumentException("Description must not exceed 500 characters");
                    }

                    // Type: Required, Enum Valid
                    if (dto.getTypeName() == null || dto.getTypeName().isBlank()) {
                        throw new IllegalArgumentException("Type is required");
                    }
                    CategoryType type;
                    try {
                        type = CategoryType.valueOf(dto.getTypeName().toUpperCase().trim());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid Type. Must be INCOME or EXPENSE");
                    }

                    // Duplicate Check (DB & Internal File)
                    if (existingNamesInDb.contains(dto.getName())) {
                        throw new IllegalArgumentException("Category '" + dto.getName() + "' already exists globally");
                    }
                    if (processedNamesInCurrentFile.contains(dto.getName())) {
                        throw new IllegalArgumentException("Category '" + dto.getName() + "' is duplicated in this file");
                    }

                    // --- MAPPING ---
                    Category category = new Category();
                    category.setName(dto.getName());
                    category.setDescription(dto.getDescription());
                    category.setIcon(dto.getIcon());
                    category.setType(type);
                    category.setUser(null);
                    
                    buffer.add(category);
                    processedNamesInCurrentFile.add(dto.getName());

                    // Batch Save
                    if (buffer.size() >= BATCH_SIZE) {
                        saveBatch(buffer);
                        successCount += buffer.size();
                        buffer.clear();
                    }

                } catch (Exception ex) {
                    failCount++;
                    if (errorReport.length() < 4000) {
                        errorReport.append(String.format("Row %d: %s | ", rowNum, ex.getMessage()));
                    }
                }
            }

            // --- STEP 5: SAVE BATCH ---
            if (!buffer.isEmpty()) {
                saveBatch(buffer);
                successCount += buffer.size();
            }

            // --- STEP 6: LOG RESULT ---
            String finalMessage = String.format("Completed. Success: %d, Failed: %d. %s", 
                    successCount, failCount, 
                    failCount > 0 ? "\nErrors: " + errorReport.toString() : "");
            
            if (finalMessage.length() > 5000) finalMessage = finalMessage.substring(0, 5000) + "...";
            updateLogStatus(logId, JobStatus.SUCCESS, finalMessage);

        } catch (Exception e) {
            log.error("Async Category Import Error", e);
            updateLogStatus(logId, JobStatus.FAILED, "System Error: " + e.getMessage());
        } finally {
            csvService.cleanUpTempFile(tempFile);
        }
    }

    @Transactional
    public void saveBatch(List<Category> categories) {
        categoryRepository.saveAll(categories);
        categoryRepository.flush();
    }

    private void updateLogStatus(UUID logId, JobStatus status, String message) {
        logRepository.findById(logId).ifPresent(logItem -> {
            logItem.setStatus(status);
            logItem.setMessage(message);
            logRepository.save(logItem);
        });
    }
}
