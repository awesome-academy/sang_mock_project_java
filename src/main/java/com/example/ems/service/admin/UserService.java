package com.example.ems.service.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.ems.dto.request.UserDto;

import java.util.UUID;

public interface UserService {
	Page<UserDto> getUsers(String keyword, Boolean isActive, Pageable pageable);
	UserDto getUserById(UUID id);
	void saveUser(UserDto userDto);
	void updateUser(UUID id, UserDto userDto);
}
