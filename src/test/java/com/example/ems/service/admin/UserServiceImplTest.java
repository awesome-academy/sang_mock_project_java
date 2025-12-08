package com.example.ems.service.admin;

import com.example.ems.constant.RoleType;
import com.example.ems.dto.request.UserDto;
import com.example.ems.entity.Role;
import com.example.ems.entity.User;
import com.example.ems.repository.RoleRepository;
import com.example.ems.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    // --- TEST: Get Users (Search & Pagination) ---
    @Test
    @DisplayName("getUsers: Should return Page of UserDto")
    void getUsers_ShouldReturnPage() {
        // Arrange
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Test User");
        user.setRoles(Set.of(new Role(RoleType.ROLE_USER, "Desc")));

        Page<User> userPage = new PageImpl<>(List.of(user));
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.searchUsers(any(), any(), any())).thenReturn(userPage);

        // Act
        Page<UserDto> result = userService.getUsers("", true, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test User", result.getContent().get(0).getName());
        verify(userRepository).searchUsers(any(), any(), any());
    }

    // --- TEST: Create User ---
    @Test
    @DisplayName("saveUser: Should save successfully when email is unique")
    void saveUser_Success() {
        UserDto dto = new UserDto();
        dto.setEmail("new@example.com");
        dto.setPassword("123456");
        dto.setRoleIds(List.of(UUID.randomUUID()));

        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("encodedPass");
        when(roleRepository.findAllById(any())).thenReturn(List.of(new Role()));

        userService.saveUser(dto);

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("saveUser: Should throw exception when email exists")
    void saveUser_DuplicateEmail_ShouldThrowException() {
        UserDto dto = new UserDto();
        dto.setEmail("exist@example.com");

        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.saveUser(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUser: Should update successfully when email is not changed")
    void updateUser_NoEmailChange_Success() {
        UUID id = UUID.randomUUID();
        User existingUser = new User();
        existingUser.setId(id);
        existingUser.setEmail("old@example.com");

        UserDto dto = new UserDto();
        dto.setEmail("old@example.com");

        when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));

        userService.updateUser(id, dto);

        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("updateUser: Should check duplicate when email changed")
    void updateUser_EmailChanged_Valid_Success() {
        UUID id = UUID.randomUUID();
        User existingUser = new User();
        existingUser.setId(id);
        existingUser.setEmail("old@example.com");

        UserDto dto = new UserDto();
        dto.setEmail("new@example.com");

        when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        userService.updateUser(id, dto);

        assertEquals("new@example.com", existingUser.getEmail());
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("updateUser: Should throw exception when new email is taken")
    void updateUser_EmailChanged_Duplicate_ShouldThrow() {
        UUID id = UUID.randomUUID();
        User existingUser = new User();
        existingUser.setId(id);
        existingUser.setEmail("old@example.com");

        UserDto dto = new UserDto();
        dto.setEmail("taken@example.com");

        when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(id, dto));
        verify(userRepository, never()).save(any());
    }

    // --- TEST: Get User By ID ---
    @Test
    @DisplayName("getUserById: Should return DTO when found")
    void getUserById_Found() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        user.setEmail("test@example.com");
        user.setRoles(new HashSet<>()); // Initialize roles to avoid NPE in stream

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        UserDto result = userService.getUserById(id);

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    @DisplayName("getUserById: Should throw exception when not found")
    void getUserById_NotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.getUserById(id));
    }
}
