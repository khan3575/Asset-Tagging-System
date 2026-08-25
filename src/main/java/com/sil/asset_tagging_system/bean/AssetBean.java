package com.sil.asset_tagging_system.bean;

import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

import com.sil.asset_tagging_system.dto.AssetRow;
import com.sil.asset_tagging_system.service.AssetService;
import com.sil.asset_tagging_system.util.FacesUtil;
import com.sil.asset_tagging_system.util.PageParams;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Named
@RequestScoped
public class AssetBean {
    private final AssetService assetService;

    // variables
    private String search;
    private List<AssetRow> assetList;
    private int offset;
    private int page;
    private int totalPageCount;
    private int totalCount;
    private final int pageSize = 10;

    public AssetBean(AssetService assetService)
    {
        this.assetService = assetService;
    }

    @PostConstruct
    public void init()
    {
        Map<String, String> params = FacesUtil.getRequestParams();

        this.search = params.get("search");
        if(search == null || search.isBlank() || search.trim().isBlank())
        {
            search = "";
        }
        else{
            search = search.trim().toLowerCase();
        }
        PageParams pageParams = PageParams.parse(params, pageSize);
        page = pageParams.page;
        offset = pageParams.offset;
        log.info("AssetBean initiated - search {}", search);

        
        if(search != null && !search.trim().isEmpty())
        {
            String searchLowerCase = search.trim().toLowerCase();
            search = searchLowerCase;
        }
        
        assetList = assetService.findPage(search, pageSize, offset);
        totalCount = (int) assetService.countAssets(search);
        totalPageCount = (int) Math.ceil((double)totalCount / pageSize);
    }
}
