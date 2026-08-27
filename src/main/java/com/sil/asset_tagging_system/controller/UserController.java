package com.sil.asset_tagging_system.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller

public class UserController {

    @GetMapping("/user")
    public void getAllUserList(HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        request.getRequestDispatcher("/user-list.xhtml").forward(request, response);
    }
    @GetMapping("/user/{id}")
    public void getUser(HttpServletRequest request, HttpServletResponse response, @PathVariable String id) throws Exception
    {
        request.getRequestDispatcher("/user-detail.xhtml?id="+id).forward(request,response);
    }
}
