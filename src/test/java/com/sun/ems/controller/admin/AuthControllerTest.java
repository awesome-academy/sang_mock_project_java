package com.sun.ems.controller.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;


    @Test
    @DisplayName("Case 1: Anonymous user should be able to access login page")
    @WithAnonymousUser
    void whenAnonymous_thenAccessLoginPage() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"));
    }

    @Test
    @DisplayName("Case 2: Authenticated user (ADMIN) accessing login page should be redirected to dashboard")
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
                .andExpect(redirectedUrlPattern("**/admin/login")); 
    }


    @Test
    @DisplayName("Case 5: User with ROLE_USER (Not ADMIN) accessing dashboard should be Forbidden (403)")
    @WithMockUser(username = "user", roles = {"USER"}) 
    void whenUserNotAdmin_accessDashboard_thenForbidden() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andDo(print())
                .andExpect(status().isForbidden()); 
    }

    @Test
    @DisplayName("Case 6: Login with invalid credentials should redirect to login?error")
    void whenLoginFailed_thenRedirectToError() throws Exception {
        mockMvc.perform(post("/admin/login")
                        .param("username", "wronguser@example.com")
                        .param("password", "wrongpassword")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?error")); 
    }
    
    @Test
    @DisplayName("Case 7: Accessing login page with error param should show error message")
    void whenAccessLoginWithError_thenShowView() throws Exception {
        mockMvc.perform(get("/admin/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login")); 
    }
}