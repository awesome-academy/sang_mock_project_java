package com.example.ems.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class UserDto {

    private UUID id;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank(message = "Email is required")
    @Email
    private String email;

    private String password; 
    private String avatarUrl; 

    private Boolean isActive = true;

    private List<UUID> roleIds = new ArrayList<>();

    private List<String> roleNames = new ArrayList<>();
}