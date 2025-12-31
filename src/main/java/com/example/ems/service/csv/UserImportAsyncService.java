package com.example.ems.service.csv;

import com.example.ems.constant.JobStatus;
import com.example.ems.dto.csv.UserCsvDto;
import com.example.ems.entity.Role;
import com.example.ems.entity.User;
import com.example.ems.repository.ImportExportLogRepository;
import com.example.ems.repository.RoleRepository;
import com.example.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserImportAsyncService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ImportExportLogRepository logRepository;
    private final PasswordEncoder passwordEncoder;
    private final CsvService csvService;
    
    private static final String[] EXPECTED_HEADERS = {"FULL NAME", "EMAIL ADDRESS", "ROLE", "ACTIVE"};

    @Async
    @Transactional
    public void processImport(UUID logId, File tempFile) {
        StringBuilder errorReport = new StringBuilder();
        int successCount = 0;
        int failCount = 0;

        try {
            updateLogStatus(logId, JobStatus.IN_PROGRESS, "Validating and Parsing CSV file...");

            // --- STEP 1: VALIDATE HEADERS ---
            try {
                csvService.validateHeaders(tempFile, EXPECTED_HEADERS);
            } catch (Exception e) {
                log.warn("Header validation failed for logId {}: {}", logId, e.getMessage());
                updateLogStatus(logId, JobStatus.FAILED, "CSV Header Error: " + e.getMessage());
                return;
            }

            // --- STEP 2: PARSE CSV ---
            List<UserCsvDto> dtos = csvService.readCsvFromFile(tempFile, UserCsvDto.class);

            // --- STEP 3: PREPARE DATA ---
            Map<String, Role> roleMap = roleRepository.findAll().stream()
                    .collect(Collectors.toMap(r -> r.getName().name(), r -> r));
            
            Set<String> existingEmails = new HashSet<>(userRepository.findAllEmails());
            List<User> usersToSave = new ArrayList<>();

            // --- STEP 4: PROCESS ROWS ---
            for (int i = 0; i < dtos.size(); i++) {
                UserCsvDto dto = dtos.get(i);
                int rowNum = i + 2; // +1 header, +1 index

                // Validate Email required
                if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
                    errorReport.append(String.format("Row %d: Email is required. | ", rowNum));
                    failCount++;
                    continue;
                }
                
                // Validate Duplicate DB
                if (existingEmails.contains(dto.getEmail())) {
                    errorReport.append(String.format("Row %d: Email '%s' already exists. | ", rowNum, dto.getEmail()));
                    failCount++;
                    continue;
                }
                
                // Validate Duplicate in CSV file
                if (usersToSave.stream().anyMatch(u -> u.getEmail().equals(dto.getEmail()))) {
                     errorReport.append(String.format("Row %d: Duplicate email '%s' inside file. | ", rowNum, dto.getEmail()));
                     failCount++;
                     continue;
                }

                try {
                    User user = new User();
                    user.setName(dto.getName() != null ? dto.getName() : "Unknown");
                    user.setEmail(dto.getEmail());
                    user.setIsActive(dto.isActive());
                    user.setPassword(passwordEncoder.encode("123456"));

                    // Map Roles
                    Set<Role> roles = new HashSet<>();
                    if (dto.getRoleName() != null && !dto.getRoleName().trim().isEmpty()) {
                        for (String rName : dto.getRoleName().split(",")) {
                            String cleanName = rName.trim();
                            if (roleMap.containsKey(cleanName)) {
                                roles.add(roleMap.get(cleanName));
                            }
                        }
                    }
                    
                    if (roles.isEmpty() && roleMap.containsKey("ROLE_USER")) {
                        roles.add(roleMap.get("ROLE_USER"));
                    }
                    user.setRoles(roles);

                    usersToSave.add(user);
                    successCount++;

                } catch (Exception ex) {
                    errorReport.append(String.format("Row %d: Data error (%s). | ", rowNum, ex.getMessage()));
                    failCount++;
                }
            }

            // --- STEP 5: SAVE BATCH ---
            if (!usersToSave.isEmpty()) {
                userRepository.saveAll(usersToSave);
            }

            // --- STEP 6: LOG RESULT ---
            String finalMessage = String.format("Completed. Success: %d, Failed: %d. %s", 
                    successCount, failCount, 
                    failCount > 0 ? "\nDetails: " + errorReport.toString() : "");
            
            if (finalMessage.length() > 5000) finalMessage = finalMessage.substring(0, 5000) + "... [truncated]";

            JobStatus finalStatus = (successCount == 0 && failCount > 0) ? JobStatus.FAILED : JobStatus.SUCCESS;
            updateLogStatus(logId, finalStatus, finalMessage);

        } catch (Exception e) {
            log.error("Async Import Error", e);
            updateLogStatus(logId, JobStatus.FAILED, "System Error: " + e.getMessage());
        } finally {
        	csvService.cleanUpTempFile(tempFile);
        }
    }

    private void updateLogStatus(UUID logId, JobStatus status, String message) {
        logRepository.findById(logId).ifPresent(logItem -> {
            logItem.setStatus(status);
            logItem.setMessage(message);
            logRepository.save(logItem);
        });
    }
}
