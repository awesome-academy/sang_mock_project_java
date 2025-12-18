package com.example.ems.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.ems.constant.RoleType;
import com.example.ems.entity.Role;
import com.example.ems.entity.User;
import com.example.ems.repository.RoleRepository;
import com.example.ems.repository.UserRepository;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (roleRepository.count() == 0) {
            createRole(RoleType.ROLE_ADMIN, "Administrator with full access");
            createRole(RoleType.ROLE_USER, "Standard user with access to personal data");
        }

        if (!userRepository.existsByEmail("admin@ems.com")) {
            createUser("Super Admin", "admin@ems.com", "123456", RoleType.ROLE_ADMIN);
        }

        if (!userRepository.existsByEmail("user@ems.com")) {
            createUser("Demo User", "user@ems.com", "123456", RoleType.ROLE_USER);
        }
    }

    private void createRole(RoleType name, String description) {
        Role role = Role.builder()
                .name(name)
                .description(description)
                .build();
        roleRepository.save(role);
    }

    private void createUser(String name, String email, String password, RoleType roleType) {
        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(password)) // Hash password
                .isActive(true)
                .roles(Set.of(role))
                .build();
        
        userRepository.save(user);
        System.out.println(">>> Generated User: " + email +  " created.");
    }
}
