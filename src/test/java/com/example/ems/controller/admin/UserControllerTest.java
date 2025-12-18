package com.example.ems.controller.admin;

import com.example.ems.dto.request.UserDto;
import com.example.ems.repository.RoleRepository;
import com.example.ems.service.admin.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private RoleRepository roleRepository;

    @Test
    @DisplayName("GET /users should return list view")
    @WithMockUser(roles = "ADMIN")
    void listUsers_ShouldReturnView() throws Exception {
        Page<UserDto> page = new PageImpl<>(new ArrayList<>());
        when(userService.getUsers(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/list"))
                .andExpect(model().attributeExists("users"));
    }

    @Test
    @DisplayName("POST /users/save with valid data should redirect")
    @WithMockUser(roles = "ADMIN")
    void saveUser_ValidData_ShouldRedirect() throws Exception {
        doNothing().when(userService).saveUser(any(UserDto.class));

        mockMvc.perform(post("/admin/users/save")
                        .param("name", "Test User")
                        .param("email", "test@example.com")
                        .param("password", "123456")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("POST /users/save with invalid data should return form with errors")
    @WithMockUser(roles = "ADMIN")
    void saveUser_InvalidData_ShouldReturnForm() throws Exception {
        mockMvc.perform(post("/admin/users/save")
                        .param("name", "Test User") 
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/form"))
                .andExpect(model().attributeHasFieldErrors("userDto", "email"));
    }
}
