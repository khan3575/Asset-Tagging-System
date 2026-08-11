package com.sil.asset_tagging_system.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;

import java.io.IOException;

@Controller
public class DashboardController {
    public void getDashboard(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        request.getRequestDispatcher("/dashboard.xhtml").forward(request, response);
    }
}
