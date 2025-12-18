package com.example.ems.service.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.ems.dto.request.UserDto;
import com.example.ems.entity.Role;
import com.example.ems.entity.User;
import com.example.ems.exception.EmailAlreadyExistsException;
import com.example.ems.exception.ResourceNotFoundException;
import com.example.ems.repository.RoleRepository;
import com.example.ems.repository.UserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<UserDto> getUsers(String keyword, Boolean isActive, Pageable pageable) {
        Page<User> userPage = userRepository.searchUsers(keyword, isActive, pageable);
        
        return userPage.map(this::mapToDto);
    }

    @Override
    public UserDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToDto(user);
    }

    @Override
    @Transactional
    public void saveUser(UserDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists: " + dto.getEmail());
        }
        User user = new User();
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new IllegalArgumentException("Password is required for new user");
        }
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        mapToEntity(dto, user);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateUser(UUID id, UserDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (!user.getEmail().equals(dto.getEmail())) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new EmailAlreadyExistsException("Email already exists: " + dto.getEmail());
            }
        }
        
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        mapToEntity(dto, user);
        userRepository.save(user);
    }
    
    private void mapToEntity(UserDto dto, User user) {
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setIsActive(dto.getIsActive());

        if (dto.getRoleIds() != null) {
            List<Role> roles = roleRepository.findAllById(dto.getRoleIds());
            user.setRoles(new HashSet<>(roles));
        }
    }

    private UserDto mapToDto(User user) {
    	UserDto dto = new UserDto();
    	
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setIsActive(user.getIsActive());
        dto.setAvatarUrl(user.getAvatarUrl());
        
        dto.setRoleIds(user.getRoles().stream()
                .map(Role::getId)
                .collect(Collectors.toList()));
                
        dto.setRoleNames(user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList()));
                
        return dto;
    }
}
