package com.sil.asset_tagging_system.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AssetController {

    @GetMapping("/assets")
    public void getAsset(HttpServletRequest request , HttpServletResponse response) throws Exception
    {
        request.getRequestDispatcher("/asset-list.xhtml").forward(request, response);
    }

    @GetMapping("/assets/new")
    public void getAssetForm(HttpServletRequest request , HttpServletResponse response) throws Exception
    {
        request.getRequestDispatcher("/add-asset.xhtml").forward(request, response);
    }

    @GetMapping("/assets/{id}")
    public void getAssetView(HttpServletRequest request, HttpServletResponse response, @PathVariable String id) throws Exception
    {
        request.getRequestDispatcher("/asset-view.xhtml?id="+id).forward(request,response);
    }

    @GetMapping("/scope-test")
    public void getScope(HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        request.getRequestDispatcher("/scope-test.xhtml").forward(request,response);
    }
}
