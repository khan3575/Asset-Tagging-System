package com.sil.asset_tagging_system.controller;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    @GetMapping("/")
    public String redirectToDashboard()
    {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public void getDashboard(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        request.getRequestDispatcher("/dashboard.xhtml").forward(request, response);
    }
}
