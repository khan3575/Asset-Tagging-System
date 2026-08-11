package com.sil.asset_tagging_system.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AssetController {

    @GetMapping("/assets")
    public void getAsset(HttpServletRequest request , HttpServletResponse response) throws Exception
    {
        request.getRequestDispatcher("/asset-list.xhtml").forward(request, response);
    }
    
}
