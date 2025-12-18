package com.example.ems.controller.admin;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AuthController {

    @GetMapping("/login")
    public String loginPage() {
    	Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null 
			&& authentication.isAuthenticated() 
			&& !(authentication instanceof AnonymousAuthenticationToken)
		) {
            return "redirect:/admin/dashboard";
        }
        
        return "admin/login";
    }
}
