package com.sil.asset_tagging_system.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller

public class UserListViewController {

    @GetMapping("/user-list")
    public void getUserView(HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        request.getRequestDispatcher("/user-list.xhtml").forward(request, response);
    }

}
