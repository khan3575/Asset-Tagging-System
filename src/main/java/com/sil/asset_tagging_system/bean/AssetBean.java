package com.sil.asset_tagging_system.bean;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import com.sil.asset_tagging_system.dao.AssetDao;
import com.sil.asset_tagging_system.model.Asset;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Named
@RequestScoped
public class AssetBean {
    private final AssetDao assetDao;

    // variables
    private String search;
    private List<Asset> all;
    private List<Asset> filteredList;
    private List<Asset> assetList;
    private int offset;
    private int page;
    private int totalPageCount;
    private int totalCount;
    private final int pageSize = 10;
    public AssetBean(AssetDao assetDao)
    {
        this.assetDao = assetDao;
    }

    @PostConstruct
    public void init()
    {
        Map<String, String> params = FacesContext.getCurrentInstance()
                                    .getExternalContext()
                                    .getRequestParameterMap();

        this.search = params.get("search");
        if(search == null || search.isBlank() || search.trim().isBlank())
        {
            search = "";
        }
        else{
            search = search.trim().toLowerCase();
        }
        String pageParam = params.get("page");
        if(pageParam == null || pageParam.isBlank())
        {
            page = 1;
            offset = 0;
        }
        else{
            if(pageParam.matches("\\d+"))
            {
                page = Integer.valueOf(pageParam);
                offset = (page - 1) * pageSize;
            }
            else{
                page = 1;
                offset = 0;
            }
        }
        log.info("AssetBean initiated - search {}", search);

        all = assetDao.findAll();
        
        if(search != null || !search.trim().isEmpty())
        {
            String searchLowerCase = search.trim().toLowerCase();

           
            filteredList = all.stream().filter(
                asset -> (asset.getAssetTag() != null && asset.getAssetTag().toLowerCase().contains(searchLowerCase)) || 
                (asset.getName() != null && asset.getName().toLowerCase().contains(searchLowerCase) )
            ).collect(Collectors.toList());
        }
        totalCount = filteredList.size();
        totalPageCount = (int) Math.ceil(totalCount / pageSize);

        int start = Math.min(offset, totalCount);
        int end = Math.min(start + pageSize, totalCount);

        
        assetList = filteredList.subList(start, end);
    }
    
    
}
