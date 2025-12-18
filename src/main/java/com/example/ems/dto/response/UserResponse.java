
package com.example.ems.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String name;
    private String email;
    private String avatarUrl;
    private List<String> roles;
    private Boolean isActive;
}
