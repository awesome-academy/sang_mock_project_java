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
import com.sun.ems.security.jwt.JwtUtils;
import com.sun.ems.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public JwtResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtils.generateJwtToken(authentication);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(item -> item.getAuthority())
                .orElse("ROLE_NONE");

        return JwtResponse.builder()
                .token(jwt)
                .type("Bearer")
                .id(userDetails.getUser().getId())
                .name(userDetails.getUser().getName())
                .email(userDetails.getUsername())
                .role(role)
                .build();
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email is already in use!");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .isActive(true)
                .build();

        Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                .orElseThrow(() -> new ResourceNotFoundException("Role is not found."));
        user.setRoles(Set.of(userRole));

        userRepository.save(user);
    }
}
