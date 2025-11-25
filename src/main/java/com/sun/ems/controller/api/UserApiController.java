package com.sun.ems.controller.api;

import com.sun.ems.dto.response.ApiResponse;
import com.sun.ems.dto.response.UserResponse;
import com.sun.ems.service.api.UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile() {
        UserResponse profile = userService.getMyProfile();
        
        ApiResponse<UserResponse> response = ApiResponse.<UserResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Success")
                .data(profile)
                .build();

        return ResponseEntity.ok(response);
    }
}
