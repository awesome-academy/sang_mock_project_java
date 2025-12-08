package com.example.ems.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class JwtResponse {
    private String token;
    private String type;
    private UUID id;
    private String name;
    private String email;
    private String role;
}
