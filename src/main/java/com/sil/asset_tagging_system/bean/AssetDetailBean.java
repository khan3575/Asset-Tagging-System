package com.sil.asset_tagging_system.bean;

import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sil.asset_tagging_system.dao.AssetDao;
import com.sil.asset_tagging_system.exception.DbFetchException;
import com.sil.asset_tagging_system.model.Asset;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Named
@RequestScoped
public class AssetDetailBean {
    private final AssetDao assetDao;
    private Long id;
    private Asset asset;

    @Inject
    public AssetDetailBean(AssetDao assetDao)
    {
        this.assetDao = assetDao;
    }

    @PostConstruct
    public void init()
    {
        Map<String, String> params = FacesContext.getCurrentInstance()
                                    .getExternalContext()
                                    .getRequestParameterMap();
        String idParam = params.get("id");
        if(idParam == null || idParam.isBlank()){
            log.info("Asset is null : findById()");
            asset=null;
        }
        else{
            this.id= Long.valueOf(idParam);
            this.asset = assetDao.findById(id).orElseThrow(() -> new DbFetchException("Asset Fetching error from DB"));
            log.info("Asset is loaded - asset id : "+id);
        }
    }
}
