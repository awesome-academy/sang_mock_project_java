package com.sun.ems.controller.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Case 1: Anonymous user should be able to access login page")
    void whenAnonymous_thenAccessLoginPage() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"));
    }

    @Test
    @DisplayName("Case 2: Authenticated user (ADMIN) should be redirected to dashboard")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void whenAuthenticated_thenRedirectToDashboard() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().is3xxRedirection()) 
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @DisplayName("Case 3: Authenticated user accessing dashboard should succeed")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void whenAuthenticated_accessDashboard_thenSuccess() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
    }
    
    @Test
    @DisplayName("Case 4: Anonymous user accessing dashboard should be redirected to login")
    void whenAnonymous_accessDashboard_thenRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/admin/login")); 
    }
}