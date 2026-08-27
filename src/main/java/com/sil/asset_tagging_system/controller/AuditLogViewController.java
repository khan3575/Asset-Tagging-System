package com.sil.asset_tagging_system.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuditLogViewController {

    @GetMapping("/audit-log")
    public void viewAuditLog(HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        request.getRequestDispatcher("/activity/log.xhtml").forward(request,response);
    }
}
