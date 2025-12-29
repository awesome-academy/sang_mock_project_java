package com.example.ems.service.csv;

import com.example.ems.constant.EntityType;
import com.example.ems.constant.LogAction;
import com.example.ems.dto.csv.UserCsvDto;
import com.example.ems.entity.Role;
import com.example.ems.entity.User;
import com.example.ems.repository.RoleRepository;
import com.example.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCsvServiceImpl {

    private final UserRepository userRepository;
    private final CsvService csvService;
    private final ImportExportLogService logService;

    @Transactional(readOnly = true)
    public void exportUsers(HttpServletResponse response, String keyword, Boolean isActive) {
        String fileName = "users_export_" + System.currentTimeMillis() + ".csv";
        UUID logId = null;

        try {
            logId = logService.startLog(LogAction.EXPORT, EntityType.USER, fileName);

            try (Stream<User> userStream = userRepository.streamUsersForExport(keyword, isActive)) {
                Stream<UserCsvDto> dtoStream = userStream.map(this::mapToCsvDto);
                csvService.exportToCsvStream(response, dtoStream, UserCsvDto.class, fileName);
            }

            logService.finishLogSuccess(logId, "Export users successfully (Streaming mode).");

        } catch (Exception e) {
            log.error("Export failed", e);
            if (logId != null) {
                logService.finishLogFailed(logId, "Export failed: " + e.getMessage());
            }
            throw new RuntimeException(e);
        }
    }

    private UserCsvDto mapToCsvDto(User user) {
        return new UserCsvDto(
                user.getName(),
                user.getEmail(),
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.joining(", ")),
                user.getIsActive()
        );
    }
}