package com.sil.asset_tagging_system.bean;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sil.asset_tagging_system.dto.Actor;
import com.sil.asset_tagging_system.exception.BusinessRuleException;
import com.sil.asset_tagging_system.exception.DuplicateAssetTagException;
import com.sil.asset_tagging_system.service.AssetService;

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
            assetService.register(assetTag, name, categoryId, purchaseDate, value, Actor.current());
        }
        catch (DuplicateAssetTagException e)
        {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Asset tag already exists", null));
            return null;
        }
        catch (BusinessRuleException e)
        {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
            return null;
        }
        return "/asset/list?faces-redirect=true";
    }
}
