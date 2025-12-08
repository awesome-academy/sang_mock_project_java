package com.example.ems.service.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.example.ems.constant.RoleType;
import com.example.ems.dto.response.UserResponse;
import com.example.ems.entity.Role;
import com.example.ems.entity.User;
import com.example.ems.repository.UserRepository;
import com.example.ems.service.api.UserService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;
    private SecurityContext securityContext;
    private Authentication authentication;

    private User mockUser;
    private final String TEST_EMAIL = "test@email.com";

    @BeforeEach
    void setUp() {
        authentication = mock(Authentication.class);
        securityContext = mock(SecurityContext.class);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        
        SecurityContextHolder.setContext(securityContext);

        Role userRole = Role.builder().name(RoleType.ROLE_USER).build();
        mockUser = User.builder()
                .name("Test User")
                .email(TEST_EMAIL)
                .avatarUrl("http://avatar.url")
                .isActive(true)
                .roles(Set.of(userRole))
                .build();
        mockUser.setId(UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Get Profile: Should return user info when user exists")
    void getMyProfile_Success() {
        // GIVEN
        when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));

        // WHEN
        UserResponse response = userService.getMyProfile();

        // THEN
        assertNotNull(response);
        assertEquals(mockUser.getId(), response.getId());
        assertEquals(mockUser.getEmail(), response.getEmail());
        assertEquals(mockUser.getName(), response.getName());
        assertEquals(mockUser.getAvatarUrl(), response.getAvatarUrl());
        assertTrue(response.getIsActive());
        
        assertNotNull(response.getRoles());
        assertEquals(1, response.getRoles().size());
        assertEquals("ROLE_USER", response.getRoles().get(0));
    }

    @Test
    @DisplayName("Get Profile: Should throw exception when user not found in DB")
    void getMyProfile_UserNotFound() {
        // GIVEN
        when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // WHEN & THEN
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userService.getMyProfile();
        });

        assertEquals("User not found", exception.getMessage());
    }
    
    @Test
    @DisplayName("Get Profile: Should handle user with multiple roles correctly")
    void getMyProfile_MultipleRoles() {
        // GIVEN
        Role adminRole = Role.builder().name(RoleType.ROLE_ADMIN).build();
        Role userRole = Role.builder().name(RoleType.ROLE_USER).build();
        
        mockUser.setRoles(Set.of(adminRole, userRole));

        when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));

        // WHEN
        UserResponse response = userService.getMyProfile();

        // THEN
        assertEquals(2, response.getRoles().size());
        List<String> roles = response.getRoles();
        assertTrue(roles.contains("ROLE_ADMIN"));
        assertTrue(roles.contains("ROLE_USER"));
    }
}
