package com.sun.ems.controller.api;

import com.sun.ems.dto.request.LoginRequest;
import com.sun.ems.dto.request.RegisterRequest;
import com.sun.ems.dto.response.ApiResponse;
import com.sun.ems.service.api.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
    	var jwtResponse = authService.login(loginRequest);
        ApiResponse<Object> response = ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Login successful!")
                .data(jwtResponse)
                .build();
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.register(registerRequest);
        ApiResponse<Object> response = ApiResponse.builder()
                .status(HttpStatus.CREATED.value())
                .message("User registered successfully!")
                .data(null)
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);    
    }
}
