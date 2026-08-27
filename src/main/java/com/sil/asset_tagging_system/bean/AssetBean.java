package com.sil.asset_tagging_system.bean;

import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

import com.sil.asset_tagging_system.dto.AssetRow;
import com.sil.asset_tagging_system.service.AssetService;
import com.sil.asset_tagging_system.util.PageParams;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Named
@RequestScoped
public class AssetBean {
    private final AssetService assetService;

    // variables
    @Setter
    private String search;

    private List<AssetRow> assetList;
    private int offset;

    @Setter
    private Integer page;
    private int totalPageCount;
    private int totalCount;
    private final int pageSize = 10;

    public AssetBean(AssetService assetService)
    {
        this.assetService = assetService;
    }

    public void load()
    {

        page = PageParams.clamp(page);
        offset = PageParams.offset(page, pageSize);
        if(search == null)
        {
            search = "";
        }
        else{
            search = search.trim().toLowerCase();
        }
        log.info("AssetBean initiated - search {}", search);
        
        assetList = assetService.findPage(search, pageSize, offset);
        totalCount = (int) assetService.countAssets(search);
        totalPageCount = (int) Math.ceil((double)totalCount / pageSize);
    }

    public String search()
    {
        page = 1;
        return "/asset/list?faces-redirect=true&includeViewParams=true";
    }
}
