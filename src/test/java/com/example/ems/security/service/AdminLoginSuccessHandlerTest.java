package com.example.ems.security.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminLoginSuccessHandlerTest {

    @InjectMocks
    private AdminLoginSuccessHandler successHandler;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private HttpSession session;

    @Test
    @DisplayName("Admin user should be redirected to dashboard")
    void onAuthenticationSuccess_AdminUser_RedirectsToDashboard() throws Exception {
        // Arrange
        // Mock authentication to return ROLE_ADMIN
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(authentication).getAuthorities();

        // Act
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        // Verify redirect happens to dashboard
        verify(response).sendRedirect("/admin/dashboard");
        // Verify session invalidation did NOT happen
        verify(request, never()).getSession();
    }

    @Test
    @DisplayName("Non-admin user should be rejected and redirected to login error")
    void onAuthenticationSuccess_NonAdminUser_InvalidatesSessionAndRedirects() throws Exception {
        // Arrange
        // Mock authentication to return only ROLE_USER
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .when(authentication).getAuthorities();
        
        // Mock request.getSession() to return our mocked session
        when(request.getSession()).thenReturn(session);

        // Act
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        // 1. Verify session was invalidated
        verify(session).invalidate();
        verify(response).sendRedirect("/admin/login?error");
    }
}
