package com.sun.ems.service.api;

import com.sun.ems.constant.RoleType;
import com.sun.ems.dto.request.LoginRequest;
import com.sun.ems.dto.request.RegisterRequest;
import com.sun.ems.dto.response.JwtResponse;
import com.sun.ems.entity.Role;
import com.sun.ems.entity.User;
import com.sun.ems.exception.EmailAlreadyExistsException;
import com.sun.ems.exception.ResourceNotFoundException;
import com.sun.ems.repository.RoleRepository;
import com.sun.ems.repository.UserRepository;
import com.sun.ems.security.service.CustomUserDetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private com.sun.ems.security.jwt.JwtUtils jwtUtils;
    
    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRequest;
    private Role userRole;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest();
        validRequest.setName("Test User");
        validRequest.setEmail("test@email.com");
        validRequest.setPassword("password123");

        userRole = Role.builder().name(RoleType.ROLE_USER).build();
    }

    @Test
    @DisplayName("Should register user successfully when data is valid")
    void register_Success() {
        // GIVEN
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleType.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(validRequest.getPassword())).thenReturn("encoded_password");

        // WHEN
        authService.register(validRequest);

        // THEN
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("Test User", savedUser.getName());
        assertEquals("test@email.com", savedUser.getEmail());
        assertEquals("encoded_password", savedUser.getPassword());
        assertTrue(savedUser.getIsActive());
        assertTrue(savedUser.getRoles().contains(userRole));
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void register_Fail_DuplicateEmail() {
        // GIVEN
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

        // WHEN & THEN
        EmailAlreadyExistsException exception = assertThrows(EmailAlreadyExistsException.class, () -> {
            authService.register(validRequest);
        });

        assertEquals("Email is already in use!", exception.getMessage());
        
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when ROLE_USER not found in DB")
    void register_Fail_RoleNotFound() {
        // GIVEN
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleType.ROLE_USER)).thenReturn(Optional.empty());

        // WHEN & THEN
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            authService.register(validRequest);
        });

        assertEquals("Role is not found.", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should login successfully and return JWT token")
    void login_Success() {
        // GIVEN
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@email.com");
        loginRequest.setPassword("password123");

        Authentication authentication = mock(Authentication.class);
        
        User user = User.builder()
                .email("test@email.com")
                .name("Test User")
                .isActive(true)
                .roles(Set.of(userRole))
                .build();
        user.setId(UUID.randomUUID());
        CustomUserDetails userDetails = new CustomUserDetails(user);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("mock_jwt_token_string");

        // WHEN
        JwtResponse response = authService.login(loginRequest);

        // THEN
        assertNotNull(response);
        assertEquals("mock_jwt_token_string", response.getToken());
        assertEquals("test@email.com", response.getEmail());
        assertEquals("ROLE_USER", response.getRole());
        assertEquals("Bearer", response.getType());
        
        // Verify dependencies called
        verify(authenticationManager, times(1)).authenticate(any());
        verify(jwtUtils, times(1)).generateJwtToken(authentication);
    }

    @Test
    @DisplayName("Should throw exception when login fails (Invalid Credentials)")
    void login_Fail_BadCredentials() {
        // GIVEN
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("wrong@email.com");
        loginRequest.setPassword("wrongpass");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // WHEN & THEN
        assertThrows(BadCredentialsException.class, () -> {
            authService.login(loginRequest);
        });

        verify(jwtUtils, never()).generateJwtToken(any());
    }
    
    @Test
    @DisplayName("Login: Should return ROLE_NONE if user has no roles")
    void login_Success_NoRole() {
        // GIVEN
        LoginRequest req = new LoginRequest();
        req.setEmail("norole@email.com");
        req.setPassword("pass");

        Authentication auth = mock(Authentication.class);
        
        User user = User.builder()
                .email("norole@email.com")
                .roles(Set.of())
                .isActive(true)
                .build();
        user.setId(UUID.randomUUID());

        CustomUserDetails userDetails = new CustomUserDetails(user);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(jwtUtils.generateJwtToken(auth)).thenReturn("token");

        // WHEN
        JwtResponse res = authService.login(req);

        // THEN
        assertEquals("ROLE_NONE", res.getRole());
        assertEquals("token", res.getToken());
    }
}
