package com.example.ems.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

@Component
@Slf4j
public class TempFileCleanupScheduler {
    private final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    
    private final long MAX_AGE_HOURS = 24;

    @Scheduled(cron = "0 0 2 * * ?") 
    public void cleanUpOrphanedTempFiles() {
        log.info("Starting scheduled temp file cleanup in: {}", TEMP_DIR);
        
        File dir = new File(TEMP_DIR);
        if (!dir.exists() || !dir.isDirectory()) return;

        File[] files = dir.listFiles((d, name) -> name.startsWith("users_import_") && name.endsWith(".csv"));

        if (files == null) return;

        int deletedCount = 0;
        int failCount = 0;

        for (File file : files) {
            if (isOldFile(file)) {
                if (file.delete()) {
                    deletedCount++;
                } else {
                    failCount++;
                    log.warn("Failed to delete orphaned file: {}", file.getName());
                }
            }
        }

        log.info("Cleanup finished. Deleted: {}, Failed: {}", deletedCount, failCount);
    }

    private boolean isOldFile(File file) {
        try {
            BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            Instant creationTime = attr.creationTime().toInstant();
            Instant threshold = Instant.now().minus(MAX_AGE_HOURS, ChronoUnit.HOURS);
            
            return creationTime.isBefore(threshold);
        } catch (IOException e) {
            log.error("Error reading file attributes: " + file.getName(), e);
            return false;
        }
    }
}
