package com.example.ems.listener;

import com.example.ems.constant.EntityType;
import com.example.ems.constant.LogAction;
import com.example.ems.repository.UserRepository;
import com.example.ems.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityEventListener {

    private final ActivityLogService activityLogService;
    private final UserRepository userRepository;

    @EventListener
    public void onAuthenticationSuccess(InteractiveAuthenticationSuccessEvent event) {
        try {
            Authentication authentication = event.getAuthentication();
            String email = authentication.getName();
            
            userRepository.findByEmail(email).ifPresent(user -> {
                activityLogService.recordActivity(
                        LogAction.LOGIN,
                        EntityType.USER,
                        user.getId(),
                        "Admin logged in via Web Form"
                );
            });
        } catch (Exception e) {
            log.error("Error logging login event", e);
        }
    }

    @EventListener
    public void onLogoutSuccess(LogoutSuccessEvent event) {
        try {
            Authentication authentication = event.getAuthentication();
            if (authentication != null) {
                String email = authentication.getName();
                userRepository.findByEmail(email).ifPresent(user -> {
                    activityLogService.recordActivity(
                            LogAction.LOGOUT,
                            EntityType.USER,
                            user.getId(),
                            "Admin logged out"
                    );
                });
            }
        } catch (Exception e) {
            log.error("Error logging logout event", e);
        }
    }
}
