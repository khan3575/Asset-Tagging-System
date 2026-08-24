package com.sil.asset_tagging_system.bean;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import com.sil.asset_tagging_system.security.SecurityUtil;
import com.sil.asset_tagging_system.service.AssetService;
import com.sil.asset_tagging_system.service.DuplicateAssetTagException;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@Named
@RequestScoped
public class AssetFormBean {

    private final AssetService assetService;

    // form fields, bound from add-asset.xhtml
    private String assetTag;
    private String name;
    private Long categoryId;
    private LocalDate purchaseDate;
    private BigDecimal value;

    @Inject
    public AssetFormBean(AssetService assetService)
    {
        this.assetService = assetService;
    }

    public String save()
    {
        try
        {
            assetService.register(assetTag, name, categoryId, purchaseDate, value,
                    SecurityUtil.currentUserId());
        }
        catch (DuplicateAssetTagException e)
        {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Asset tag already exists", null));
            return null;
        }

        try
        {
            FacesContext.getCurrentInstance().getExternalContext()
                    .redirect(getRequest().getContextPath() + "/assets");
        }
        catch (IOException e)
        {
            log.error("Failed to redirect after creating asset {}", assetTag, e);
        }
        return null;
    }

    private HttpServletRequest getRequest()
    {
        return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }

}
